package ir.coffevista.vista_native.features.feed.ui.create

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Pure-Kotlin ISO BMFF (MP4) faststart relocator.
 *
 * Moves a trailing `moov` box ahead of the first `mdat` box and adjusts every `stco` / `co64`
 * chunk offset so ExoPlayer can begin HTTP streaming after reading only the beginning of the file.
 * Handles both 32-bit and 64-bit (`largesize`) top-level box headers written by Android `MPEG4Writer`.
 */
internal object Mp4FastStart {

    private data class TopLevelBox(
        val offset: Long,
        val size: Long,
        val headerSize: Int,
        val type: String,
    )

    fun isFastStart(file: File): Boolean {
        if (!file.exists() || file.length() < 16L) return false
        val boxes = runCatching {
            RandomAccessFile(file, "r").use(::parseTopLevelBoxes)
        }.getOrNull() ?: return false
        val moovIndex = boxes.indexOfFirst { it.type == "moov" }
        val mdatIndex = boxes.indexOfFirst { it.type == "mdat" }
        return moovIndex >= 0 && mdatIndex >= 0 && moovIndex < mdatIndex
    }

    /**
     * Relocates `moov` before the first `mdat` into [output].
     *
     * Returns `true` when [output] was written with `moov` relocated before `mdat`.
     * Returns `false` (cleaning up [output]) if [input] is already faststart, malformed, has a `moov`
     * exceeding [MAX_MOOV_BYTES], or would overflow 32-bit `stco` chunk offsets.
     */
    fun relocateMoovToFront(input: File, output: File): Boolean {
        if (!input.exists() || input.length() < 16L) return false
        var wroteOutput = false
        return try {
            RandomAccessFile(input, "r").use { raf ->
                val boxes = parseTopLevelBoxes(raf) ?: return false
                val firstMdatIndex = boxes.indexOfFirst { it.type == "mdat" }
                val moovIndex = boxes.indexOfFirst { it.type == "moov" }
                if (firstMdatIndex < 0 || moovIndex < 0 || moovIndex < firstMdatIndex) {
                    return false
                }
                // Only handle standard single-moov MP4s safely.
                if (boxes.count { it.type == "moov" } != 1) {
                    return false
                }

                val firstMdat = boxes[firstMdatIndex]
                val moovBox = boxes[moovIndex]
                if (moovBox.size !in 8L..MAX_MOOV_BYTES) {
                    return false
                }

                val moovBytes = ByteArray(moovBox.size.toInt())
                raf.seek(moovBox.offset)
                raf.readFully(moovBytes)

                val shiftRange = firstMdat.offset until moovBox.offset
                if (!patchChunkOffsets(moovBytes, shiftRange, moovBox.size)) {
                    return false
                }

                output.parentFile?.mkdirs()
                val copyBuffer = ByteArray(STREAM_BUFFER_BYTES)
                BufferedOutputStream(FileOutputStream(output), STREAM_BUFFER_BYTES).use { out ->
                    for (index in 0 until firstMdatIndex) {
                        val box = boxes[index]
                        copyFileRange(raf, box.offset, box.size, out, copyBuffer)
                    }
                    out.write(moovBytes)
                    for (index in firstMdatIndex until boxes.size) {
                        if (index == moovIndex) continue
                        val box = boxes[index]
                        copyFileRange(raf, box.offset, box.size, out, copyBuffer)
                    }
                    out.flush()
                }
                wroteOutput = true
            }

            if (output.length() == input.length() && isFastStart(output)) {
                true
            } else {
                output.delete()
                false
            }
        } catch (_: Throwable) {
            if (wroteOutput || output.exists()) {
                runCatching { output.delete() }
            }
            false
        }
    }

    private fun parseTopLevelBoxes(raf: RandomAccessFile): List<TopLevelBox>? {
        val fileLen = raf.length()
        if (fileLen < 8L) return null
        val boxes = ArrayList<TopLevelBox>(8)
        val header = ByteArray(16)
        var offset = 0L
        while (offset + 8L <= fileLen) {
            raf.seek(offset)
            if (raf.read(header, 0, 8) != 8) return null
            val size32 = readUInt32(header, 0)
            val type = String(header, 4, 4, Charsets.US_ASCII)
            val headerSize: Int
            val boxSize: Long
            when (size32) {
                0L -> {
                    headerSize = 8
                    boxSize = fileLen - offset
                }
                1L -> {
                    if (offset + 16L > fileLen) return null
                    if (raf.read(header, 8, 8) != 8) return null
                    headerSize = 16
                    boxSize = readInt64(header, 8)
                    if (boxSize < 16L) return null
                }
                else -> {
                    headerSize = 8
                    boxSize = size32
                    if (boxSize < 8L) return null
                }
            }
            if (offset + boxSize > fileLen) return null
            boxes += TopLevelBox(offset = offset, size = boxSize, headerSize = headerSize, type = type)
            offset += boxSize
        }
        return if (offset == fileLen && boxes.isNotEmpty()) boxes else null
    }

    private fun patchChunkOffsets(
        moovBytes: ByteArray,
        shiftRange: LongRange,
        moovSize: Long,
    ): Boolean {
        if (moovBytes.size < 8) return false
        val headerSize = boxHeaderSize(moovBytes, 0, moovBytes.size) ?: return false
        return patchContainerChildren(moovBytes, headerSize, moovBytes.size, shiftRange, moovSize)
    }

    private fun patchContainerChildren(
        bytes: ByteArray,
        start: Int,
        end: Int,
        shiftRange: LongRange,
        moovSize: Long,
    ): Boolean {
        var cursor = start
        while (cursor + 8 <= end) {
            val size32 = readUInt32(bytes, cursor)
            val type = String(bytes, cursor + 4, 4, Charsets.US_ASCII)
            val headerSize = boxHeaderSize(bytes, cursor, end) ?: return false
            val boxSize = when (size32) {
                0L -> (end - cursor).toLong()
                1L -> readInt64(bytes, cursor + 8)
                else -> size32
            }
            if (boxSize < headerSize || cursor + boxSize > end) return false
            val boxEnd = (cursor + boxSize).toInt()
            val payloadStart = cursor + headerSize

            when (type) {
                "trak", "mdia", "minf", "stbl", "edts", "dinf", "udta" -> {
                    if (!patchContainerChildren(bytes, payloadStart, boxEnd, shiftRange, moovSize)) {
                        return false
                    }
                }
                "meta" -> {
                    // ISO BMFF `meta` is a FullBox (4 bytes version + flags before child atoms).
                    if (payloadStart + 4 <= boxEnd &&
                        !patchContainerChildren(bytes, payloadStart + 4, boxEnd, shiftRange, moovSize)
                    ) {
                        return false
                    }
                }
                "stco" -> {
                    if (payloadStart + 8 > boxEnd) return false
                    val entryCount = readUInt32(bytes, payloadStart + 4)
                    val entriesStart = payloadStart + 8
                    if (entryCount > Int.MAX_VALUE.toLong() || entriesStart + entryCount * 4L > boxEnd) {
                        return false
                    }
                    var off = entriesStart
                    repeat(entryCount.toInt()) {
                        val oldOffset = readUInt32(bytes, off)
                        val newOffset = if (oldOffset in shiftRange) oldOffset + moovSize else oldOffset
                        if (newOffset < 0L || newOffset > 0xFFFFFFFFL) {
                            return false
                        }
                        writeUInt32(bytes, off, newOffset)
                        off += 4
                    }
                }
                "co64" -> {
                    if (payloadStart + 8 > boxEnd) return false
                    val entryCount = readUInt32(bytes, payloadStart + 4)
                    val entriesStart = payloadStart + 8
                    if (entryCount > Int.MAX_VALUE.toLong() || entriesStart + entryCount * 8L > boxEnd) {
                        return false
                    }
                    var off = entriesStart
                    repeat(entryCount.toInt()) {
                        val oldOffset = readInt64(bytes, off)
                        if (oldOffset < 0L) return false
                        val newOffset = if (oldOffset in shiftRange) oldOffset + moovSize else oldOffset
                        if (newOffset < oldOffset) return false
                        writeInt64(bytes, off, newOffset)
                        off += 8
                    }
                }
            }
            cursor = boxEnd
        }
        return cursor == end
    }

    private fun boxHeaderSize(bytes: ByteArray, offset: Int, limit: Int): Int? {
        if (offset + 8 > limit) return null
        val size32 = readUInt32(bytes, offset)
        return if (size32 == 1L) {
            if (offset + 16 > limit) null else 16
        } else {
            8
        }
    }

    private fun copyFileRange(
        raf: RandomAccessFile,
        startOffset: Long,
        length: Long,
        out: BufferedOutputStream,
        buffer: ByteArray,
    ) {
        raf.seek(startOffset)
        var remaining = length
        while (remaining > 0L) {
            val chunk = minOf(buffer.size.toLong(), remaining).toInt()
            val read = raf.read(buffer, 0, chunk)
            if (read <= 0) throw IllegalStateException("Unexpected EOF while copying MP4 box")
            out.write(buffer, 0, read)
            remaining -= read
        }
    }

    private fun readUInt32(bytes: ByteArray, offset: Int): Long =
        ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.BIG_ENDIAN).int.toLong() and 0xFFFFFFFFL

    private fun writeUInt32(bytes: ByteArray, offset: Int, value: Long) {
        ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.BIG_ENDIAN).putInt((value and 0xFFFFFFFFL).toInt())
    }

    private fun readInt64(bytes: ByteArray, offset: Int): Long =
        ByteBuffer.wrap(bytes, offset, 8).order(ByteOrder.BIG_ENDIAN).long

    private fun writeInt64(bytes: ByteArray, offset: Int, value: Long) {
        ByteBuffer.wrap(bytes, offset, 8).order(ByteOrder.BIG_ENDIAN).putLong(value)
    }

    private const val MAX_MOOV_BYTES = 32L * 1024L * 1024L
    private const val STREAM_BUFFER_BYTES = 256 * 1024
}
