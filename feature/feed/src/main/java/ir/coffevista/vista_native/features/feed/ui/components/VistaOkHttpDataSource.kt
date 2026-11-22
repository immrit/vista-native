package ir.coffevista.vista_native.features.feed.ui.components

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.HttpUtil
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.InterruptedIOException
import java.util.Collections

/**
 * Lightweight [HttpDataSource] backed by OkHttp so ExoPlayer inherits the app's `@ExternalMedia`
 * client policy (`Proxy.NO_PROXY`, connection pool, TLS/redirect handling, HTTP/2) without adding
 * extra Media3 extension dependencies.
 */
@UnstableApi
internal class VistaOkHttpDataSource(
    private val callFactory: Call.Factory,
    private val userAgent: String? = null,
    private val defaultRequestProperties: HttpDataSource.RequestProperties? = null,
) : BaseDataSource(/* isNetwork = */ true), HttpDataSource {

    private val requestProperties = HttpDataSource.RequestProperties()

    private var dataSpec: DataSpec? = null
    private var response: Response? = null
    private var responseStream: InputStream? = null
    private var opened = false
    private var bytesToRead = 0L
    private var bytesRead = 0L

    class Factory(
        private val callFactory: Call.Factory,
        private val userAgent: String? = null,
    ) : HttpDataSource.Factory {
        private val defaultRequestProperties = HttpDataSource.RequestProperties()

        override fun createDataSource(): HttpDataSource = VistaOkHttpDataSource(
            callFactory = callFactory,
            userAgent = userAgent,
            defaultRequestProperties = defaultRequestProperties,
        )

        override fun setDefaultRequestProperties(
            defaultRequestProperties: MutableMap<String, String>,
        ): HttpDataSource.Factory {
            this.defaultRequestProperties.clearAndSet(defaultRequestProperties)
            return this
        }
    }

    override fun getUri(): Uri? =
        response?.request?.url?.toString()?.let(Uri::parse) ?: dataSpec?.uri

    override fun getResponseCode(): Int = response?.code ?: -1

    override fun getResponseHeaders(): Map<String, List<String>> =
        response?.headers?.toMultimap() ?: Collections.emptyMap()

    override fun setRequestProperty(name: String, value: String) {
        requestProperties.set(name, value)
    }

    override fun clearRequestProperty(name: String) {
        requestProperties.remove(name)
    }

    override fun clearAllRequestProperties() {
        requestProperties.clear()
    }

    @Throws(HttpDataSource.HttpDataSourceException::class)
    override fun open(dataSpec: DataSpec): Long {
        this.dataSpec = dataSpec
        bytesRead = 0L
        bytesToRead = 0L
        transferInitializing(dataSpec)

        val request = buildRequest(dataSpec)
        val executedResponse: Response = try {
            callFactory.newCall(request).execute()
        } catch (e: IOException) {
            throw HttpDataSource.HttpDataSourceException.createForIOException(
                e,
                dataSpec,
                HttpDataSource.HttpDataSourceException.TYPE_OPEN,
            )
        }
        response = executedResponse
        val body = executedResponse.body
        responseStream = body?.byteStream()

        val responseCode = executedResponse.code
        if (!executedResponse.isSuccessful) {
            if (responseCode == 416) {
                val documentSize = HttpUtil.getDocumentSize(executedResponse.header("Content-Range"))
                if (dataSpec.position == documentSize) {
                    opened = true
                    transferStarted(dataSpec)
                    return 0L
                }
            }
            val errorBody = runCatching { body?.bytes() }.getOrDefault(ByteArray(0)) ?: ByteArray(0)
            val headers = executedResponse.headers.toMultimap()
            closeConnectionQuietly()
            val cause = if (responseCode == 416) {
                androidx.media3.datasource.DataSourceException(
                    PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
                )
            } else {
                null
            }
            throw HttpDataSource.InvalidResponseCodeException(
                responseCode,
                executedResponse.message,
                cause,
                headers,
                dataSpec,
                errorBody,
            )
        }

        val bytesToSkip = if (responseCode == 200 && dataSpec.position != 0L) dataSpec.position else 0L
        bytesToRead = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            val contentLength = body?.contentLength() ?: -1L
            if (contentLength >= 0L) contentLength - bytesToSkip else C.LENGTH_UNSET.toLong()
        }

        opened = true
        transferStarted(dataSpec)

        if (bytesToSkip > 0L) {
            try {
                skipFully(bytesToSkip, dataSpec)
            } catch (e: HttpDataSource.HttpDataSourceException) {
                closeConnectionQuietly()
                throw e
            }
        }

        return bytesToRead
    }

    @Throws(HttpDataSource.HttpDataSourceException::class)
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        return try {
            readInternal(buffer, offset, length)
        } catch (e: IOException) {
            val activeSpec = dataSpec ?: DataSpec(Uri.EMPTY)
            throw HttpDataSource.HttpDataSourceException.createForIOException(
                e,
                activeSpec,
                HttpDataSource.HttpDataSourceException.TYPE_READ,
            )
        }
    }

    @Throws(HttpDataSource.HttpDataSourceException::class)
    override fun close() {
        if (opened) {
            opened = false
            transferEnded()
        }
        closeConnectionQuietly()
        dataSpec = null
    }

    private fun buildRequest(dataSpec: DataSpec): Request {
        val url = dataSpec.uri.toString().toHttpUrlOrNull()
            ?: throw HttpDataSource.HttpDataSourceException(
                "Invalid URL: ${dataSpec.uri}",
                dataSpec,
                PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK,
                HttpDataSource.HttpDataSourceException.TYPE_OPEN,
            )
        val builder = Request.Builder().url(url)

        val mergedHeaders = LinkedHashMap<String, String>()
        defaultRequestProperties?.snapshot?.let(mergedHeaders::putAll)
        mergedHeaders.putAll(requestProperties.snapshot)
        mergedHeaders.putAll(dataSpec.httpRequestHeaders)
        mergedHeaders.forEach { (name, value) -> builder.header(name, value) }

        if (mergedHeaders.keys.none { it.equals("User-Agent", ignoreCase = true) } && !userAgent.isNullOrBlank()) {
            builder.header("User-Agent", userAgent)
        }
        val rangeHeader = HttpUtil.buildRangeRequestHeader(dataSpec.position, dataSpec.length)
        if (rangeHeader != null) {
            builder.header("Range", rangeHeader)
        }
        if (!dataSpec.isFlagSet(DataSpec.FLAG_ALLOW_GZIP)) {
            builder.header("Accept-Encoding", "identity")
        }

        val requestBody = when {
            dataSpec.httpBody != null -> dataSpec.httpBody!!.toRequestBody(null)
            dataSpec.httpMethod == DataSpec.HTTP_METHOD_POST -> ByteArray(0).toRequestBody(null)
            else -> null
        }
        builder.method(dataSpec.getHttpMethodString(), requestBody)
        return builder.build()
    }

    @Throws(HttpDataSource.HttpDataSourceException::class)
    private fun skipFully(bytesToSkip: Long, dataSpec: DataSpec) {
        val stream = responseStream ?: return
        var remaining = bytesToSkip
        val skipBuf = ByteArray(SKIP_BUFFER_SIZE)
        try {
            while (remaining > 0L) {
                if (Thread.currentThread().isInterrupted) {
                    throw InterruptedIOException()
                }
                val read = stream.read(skipBuf, 0, minOf(skipBuf.size.toLong(), remaining).toInt())
                if (read == -1) {
                    throw HttpDataSource.HttpDataSourceException(
                        EOFException(),
                        dataSpec,
                        PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
                        HttpDataSource.HttpDataSourceException.TYPE_OPEN,
                    )
                }
                remaining -= read
                bytesTransferred(read)
            }
        } catch (e: HttpDataSource.HttpDataSourceException) {
            throw e
        } catch (e: IOException) {
            throw HttpDataSource.HttpDataSourceException.createForIOException(
                e,
                dataSpec,
                HttpDataSource.HttpDataSourceException.TYPE_OPEN,
            )
        }
    }

    @Throws(IOException::class)
    private fun readInternal(buffer: ByteArray, offset: Int, length: Int): Int {
        var remainingLength = length
        if (bytesToRead != C.LENGTH_UNSET.toLong()) {
            val bytesRemaining = bytesToRead - bytesRead
            if (bytesRemaining <= 0L) return C.RESULT_END_OF_INPUT
            remainingLength = minOf(remainingLength.toLong(), bytesRemaining).toInt()
        }
        val stream = responseStream ?: return C.RESULT_END_OF_INPUT
        val read = stream.read(buffer, offset, remainingLength)
        if (read == -1) return C.RESULT_END_OF_INPUT
        bytesRead += read
        bytesTransferred(read)
        return read
    }

    private fun closeConnectionQuietly() {
        runCatching { responseStream?.close() }
        runCatching { response?.close() }
        responseStream = null
        response = null
    }

    private companion object {
        const val SKIP_BUFFER_SIZE = 4096
    }
}
