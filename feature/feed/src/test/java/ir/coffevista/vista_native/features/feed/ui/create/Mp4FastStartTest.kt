package ir.coffevista.vista_native.features.feed.ui.create

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Mp4FastStartTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun relocateMoovToFront_movesTrailingMoovAndPatchesStcoOffsets() {
        val ftyp = box("ftyp", "isom".toByteArray() + ByteArray(8))
        val payloadA = byteArrayOf(11, 22, 33, 44, 55, 66, 77, 88)
        val payloadB = byteArrayOf(99, 100, 101, 102)
        val mdatHeaderSize = 8
        val originalMdatOffset = ftyp.size
        val originalChunkOffsetA = originalMdatOffset + mdatHeaderSize
        val originalChunkOffsetB = originalChunkOffsetA + payloadA.size
        val mdat = box("mdat", payloadA + payloadB)

        val stco = fullBox("stco", entryCount = 2, offsets = longArrayOf(originalChunkOffsetA.toLong(), originalChunkOffsetB.toLong()))
        val moov = box("moov", box("trak", box("mdia", box("minf", box("stbl", stco)))))

        val input = tempFolder.newFile("trailing.mp4").apply {
            writeBytes(ftyp + mdat + moov)
        }
        val output = File(tempFolder.root, "faststart.mp4")

        assertFalse(Mp4FastStart.isFastStart(input))
        assertTrue(Mp4FastStart.relocateMoovToFront(input, output))
        assertTrue(Mp4FastStart.isFastStart(output))
        assertEquals(input.length(), output.length())

        val outBytes = output.readBytes()
        val relocatedOffsets = readStcoOffsets(outBytes)
        val expectedShift = moov.size
        assertEquals(listOf(originalChunkOffsetA + expectedShift, originalChunkOffsetB + expectedShift), relocatedOffsets)

        // Exact payload bytes must appear at the patched offsets.
        val readA = outBytes.copyOfRange(relocatedOffsets[0], relocatedOffsets[0] + payloadA.size)
        val readB = outBytes.copyOfRange(relocatedOffsets[1], relocatedOffsets[1] + payloadB.size)
        assertArrayEquals(payloadA, readA)
        assertArrayEquals(payloadB, readB)
    }

    @Test
    fun relocateMoovToFront_handlesAndroid64BitMdatLargesizeAndFreeAtom() {
        val ftyp = box("ftyp", "mp42".toByteArray() + ByteArray(8))
        val freeBox = box("free", ByteArray(4))
        val mediaPayload = ByteArray(48) { (it * 3 + 7).toByte() }
        val mdat64 = box64("mdat", mediaPayload)
        val mdatPayloadOffset = ftyp.size + freeBox.size + 16
        val stco = fullBox("stco", entryCount = 1, offsets = longArrayOf(mdatPayloadOffset.toLong()))
        val moov = box("moov", box("trak", box("mdia", box("minf", box("stbl", stco)))))

        val input = tempFolder.newFile("android64.mp4").apply {
            writeBytes(ftyp + freeBox + mdat64 + moov)
        }
        val output = File(tempFolder.root, "android64_fast.mp4")

        assertFalse(Mp4FastStart.isFastStart(input))
        assertTrue(Mp4FastStart.relocateMoovToFront(input, output))
        assertTrue(Mp4FastStart.isFastStart(output))
        assertEquals(input.length(), output.length())

        val outBytes = output.readBytes()
        val relocated = readStcoOffsets(outBytes)
        assertEquals(listOf(mdatPayloadOffset + moov.size), relocated)
        assertArrayEquals(
            mediaPayload,
            outBytes.copyOfRange(relocated[0], relocated[0] + mediaPayload.size),
        )
    }

    @Test
    fun relocateMoovToFront_patchesCo64ChunkOffsets() {
        val ftyp = box("ftyp", "isom".toByteArray() + ByteArray(8))
        val payload = ByteArray(24) { (it + 1).toByte() }
        val mdat = box("mdat", payload)
        val origOffset = ftyp.size + 8L
        val co64 = fullBox64Offsets("co64", longArrayOf(origOffset))
        val moov = box("moov", box("trak", box("mdia", box("minf", box("stbl", co64)))))

        val input = tempFolder.newFile("co64.mp4").apply {
            writeBytes(ftyp + mdat + moov)
        }
        val output = File(tempFolder.root, "co64_fast.mp4")

        assertTrue(Mp4FastStart.relocateMoovToFront(input, output))
        assertTrue(Mp4FastStart.isFastStart(output))
        val outBytes = output.readBytes()
        val patched = readCo64Offsets(outBytes)
        assertEquals(listOf(origOffset + moov.size), patched)
    }

    @Test
    fun relocateMoovToFront_alreadyFastStartReturnsFalseSafely() {
        val ftyp = box("ftyp", "isom".toByteArray() + ByteArray(8))
        val stco = fullBox("stco", entryCount = 1, offsets = longArrayOf(128L))
        val moov = box("moov", box("trak", box("mdia", box("minf", box("stbl", stco)))))
        val mdat = box("mdat", ByteArray(32))

        val input = tempFolder.newFile("already_fast.mp4").apply {
            writeBytes(ftyp + moov + mdat)
        }
        val output = File(tempFolder.root, "unused.mp4")

        assertTrue(Mp4FastStart.isFastStart(input))
        assertFalse(Mp4FastStart.relocateMoovToFront(input, output))
        assertFalse(output.exists())
    }

    @Test
    fun relocateMoovToFront_corruptOrTruncatedReturnsFalseAndDeletesOutput() {
        val corrupt = tempFolder.newFile("corrupt.mp4").apply {
            writeBytes(byteArrayOf(0, 0, 0, 32, 'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte(), 1, 2))
        }
        val output = File(tempFolder.root, "bad_out.mp4")

        assertFalse(Mp4FastStart.isFastStart(corrupt))
        assertFalse(Mp4FastStart.relocateMoovToFront(corrupt, output))
        assertFalse(output.exists())
    }

    @Test
    fun relocateMoovToFront_rewritesExtendToEofMoovSizeBeforeMdat() {
        val ftyp = box("ftyp", "isom".toByteArray() + ByteArray(8))
        val payload = byteArrayOf(5, 6, 7, 8, 9, 10, 11, 12)
        val mdat = box("mdat", payload)
        val origChunkOffset = ftyp.size + 8L
        val stco = fullBox("stco", entryCount = 1, offsets = longArrayOf(origChunkOffset))
        val moov = box("moov", box("trak", box("mdia", box("minf", box("stbl", stco)))))
        // Simulate trailing moov with size32 == 0 ("extends to EOF").
        val eofMoov = moov.copyOf().apply {
            ByteBuffer.wrap(this, 0, 4).order(ByteOrder.BIG_ENDIAN).putInt(0)
        }

        val input = tempFolder.newFile("eof_moov.mp4").apply {
            writeBytes(ftyp + mdat + eofMoov)
        }
        val output = File(tempFolder.root, "eof_moov_fast.mp4")

        assertTrue(Mp4FastStart.relocateMoovToFront(input, output))
        assertTrue(Mp4FastStart.isFastStart(output))
        assertEquals(input.length(), output.length())

        val outBytes = output.readBytes()
        val relocatedMoovSize = ByteBuffer.wrap(outBytes, ftyp.size, 4).order(ByteOrder.BIG_ENDIAN).int
        assertEquals(moov.size, relocatedMoovSize)
        val patchedOffsets = readStcoOffsets(outBytes)
        assertEquals(listOf(origChunkOffset.toInt() + moov.size), patchedOffsets)
        assertArrayEquals(
            payload,
            outBytes.copyOfRange(patchedOffsets[0], patchedOffsets[0] + payload.size),
        )
    }

    private fun box(type: String, payload: ByteArray): ByteArray {
        val total = 8 + payload.size
        return ByteBuffer.allocate(total).order(ByteOrder.BIG_ENDIAN).apply {
            putInt(total)
            put(type.toByteArray(Charsets.US_ASCII))
            put(payload)
        }.array()
    }

    private fun box64(type: String, payload: ByteArray): ByteArray {
        val total = 16L + payload.size
        return ByteBuffer.allocate(16 + payload.size).order(ByteOrder.BIG_ENDIAN).apply {
            putInt(1)
            put(type.toByteArray(Charsets.US_ASCII))
            putLong(total)
            put(payload)
        }.array()
    }

    private fun fullBox(type: String, entryCount: Int, offsets: LongArray): ByteArray {
        val payload = ByteArrayOutputStream()
        val hdr = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).apply {
            putInt(0)
            putInt(entryCount)
        }.array()
        payload.write(hdr)
        offsets.forEach { offset ->
            payload.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(offset.toInt()).array())
        }
        return box(type, payload.toByteArray())
    }

    private fun fullBox64Offsets(type: String, offsets: LongArray): ByteArray {
        val payload = ByteArrayOutputStream()
        val hdr = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).apply {
            putInt(0)
            putInt(offsets.size)
        }.array()
        payload.write(hdr)
        offsets.forEach { offset ->
            payload.write(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(offset).array())
        }
        return box(type, payload.toByteArray())
    }

    private fun readStcoOffsets(mp4: ByteArray): List<Int> {
        val idx = findAtomBytes(mp4, "stco")
        val count = ByteBuffer.wrap(mp4, idx + 12, 4).order(ByteOrder.BIG_ENDIAN).int
        return (0 until count).map { i ->
            ByteBuffer.wrap(mp4, idx + 16 + i * 4, 4).order(ByteOrder.BIG_ENDIAN).int
        }
    }

    private fun readCo64Offsets(mp4: ByteArray): List<Long> {
        val idx = findAtomBytes(mp4, "co64")
        val count = ByteBuffer.wrap(mp4, idx + 12, 4).order(ByteOrder.BIG_ENDIAN).int
        return (0 until count).map { i ->
            ByteBuffer.wrap(mp4, idx + 16 + i * 8, 8).order(ByteOrder.BIG_ENDIAN).long
        }
    }

    private fun findAtomBytes(bytes: ByteArray, type: String): Int {
        val tag = type.toByteArray(Charsets.US_ASCII)
        for (i in 4 until bytes.size - 4) {
            if (bytes[i] == tag[0] && bytes[i + 1] == tag[1] && bytes[i + 2] == tag[2] && bytes[i + 3] == tag[3]) {
                return i - 4
            }
        }
        error("Atom $type not found")
    }
}
