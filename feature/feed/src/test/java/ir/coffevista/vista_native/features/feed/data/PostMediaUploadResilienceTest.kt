package ir.coffevista.vista_native.features.feed.data

import ir.coffevista.vista_native.core.network.InternalNetworkModule
import ir.coffevista.vista_native.core.network.forLargeMediaUpload
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.EOFException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.Proxy
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class PostMediaUploadResilienceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun forLargeMediaUploadDisablesTotalCallTimeoutAndPreservesProxy() {
        val baseClient = InternalNetworkModule.provideExternalMediaClient()
        val uploadClient = baseClient.forLargeMediaUpload()

        assertEquals(45_000, baseClient.callTimeoutMillis)
        assertEquals(0, uploadClient.callTimeoutMillis)
        assertEquals(30_000, uploadClient.connectTimeoutMillis)
        assertEquals(90_000, uploadClient.writeTimeoutMillis)
        assertEquals(120_000, uploadClient.readTimeoutMillis)
        assertEquals(Proxy.NO_PROXY, uploadClient.proxy)
    }

    @Test
    fun uploadWithRetryRecoversFromSocketTimeoutAndHttp503WithFreshPresign() = runTest {
        val file = tempFolder.newFile("clip.mp4").apply {
            writeBytes(ByteArray(256 * 1024) { 1 })
        }

        var presignCalls = 0
        var networkAttempts = 0
        val capturedUrls = mutableListOf<String>()
        val progressEvents = mutableListOf<Float>()

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                capturedUrls += request.url.toString()
                request.body?.writeTo(Buffer())
                networkAttempts += 1
                when (networkAttempts) {
                    1 -> throw SocketTimeoutException("Write timed out")
                    2 -> Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(503)
                        .message("Service Unavailable")
                        .body("".toResponseBody(null))
                        .build()
                    else -> Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("".toResponseBody(null))
                        .build()
                }
            }
            .build()

        val result = MediaUploadExecutor.uploadWithRetry(
            client = client,
            file = file,
            contentType = "video/mp4",
            errorLabel = "آپلود ویدیو",
            maxRetries = 2,
            backoffMs = longArrayOf(0L, 0L),
            presignProvider = {
                presignCalls += 1
                PostPresignResponseDto(
                    url = "https://storage.example.test/upload?attempt=$presignCalls",
                    headers = mapOf("x-attempt" to presignCalls.toString()),
                    objectKey = "posts/user/video.mp4",
                    objectUrl = "https://cdn.example.test/posts/user/video.mp4",
                )
            },
            onProgress = { progressEvents += it },
        )

        assertEquals(3, presignCalls)
        assertEquals(3, networkAttempts)
        assertEquals(
            listOf(
                "https://storage.example.test/upload?attempt=1",
                "https://storage.example.test/upload?attempt=2",
                "https://storage.example.test/upload?attempt=3",
            ),
            capturedUrls,
        )
        assertEquals("https://cdn.example.test/posts/user/video.mp4", result.objectUrl)
        assertTrue(progressEvents.count { it == 0f } >= 2)
        assertTrue(progressEvents.any { it > 0.5f })
    }

    @Test
    fun uploadWithRetryDoesNotRetryHttp413OrSslFailure() = runTest {
        val file = tempFolder.newFile("large.mp4").apply {
            writeBytes(ByteArray(1024) { 2 })
        }

        var http413Attempts = 0
        val http413Client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                http413Attempts += 1
                chain.request().body?.writeTo(Buffer())
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(413)
                    .message("Payload Too Large")
                    .body("".toResponseBody(null))
                    .build()
            }
            .build()

        try {
            MediaUploadExecutor.uploadWithRetry(
                client = http413Client,
                file = file,
                contentType = "video/mp4",
                errorLabel = "آپلود ویدیو",
                maxRetries = 2,
                backoffMs = longArrayOf(0L, 0L),
                presignProvider = {
                    PostPresignResponseDto(
                        url = "https://storage.example.test/upload",
                        objectKey = "posts/user/video.mp4",
                        objectUrl = "https://cdn.example.test/posts/user/video.mp4",
                    )
                },
                onProgress = {},
            )
            fail("Expected IOException for HTTP 413")
        } catch (expected: IOException) {
            assertTrue(expected.message?.contains("413") == true)
        }
        assertEquals(1, http413Attempts)

        var sslAttempts = 0
        val sslClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                sslAttempts += 1
                throw SSLHandshakeException("TLS handshake failed")
            }
            .build()

        try {
            MediaUploadExecutor.uploadWithRetry(
                client = sslClient,
                file = file,
                contentType = "video/mp4",
                errorLabel = "آپلود ویدیو",
                maxRetries = 2,
                backoffMs = longArrayOf(0L, 0L),
                presignProvider = {
                    PostPresignResponseDto(
                        url = "https://storage.example.test/upload",
                        objectKey = "posts/user/video.mp4",
                        objectUrl = "https://cdn.example.test/posts/user/video.mp4",
                    )
                },
                onProgress = {},
            )
            fail("Expected SSLHandshakeException")
        } catch (_: SSLHandshakeException) {
            // Expected
        }
        assertEquals(1, sslAttempts)
    }

    @Test
    fun toUserFriendlyUploadErrorMapsTimeoutAndNetworkErrorsToPersian() {
        val timeoutMessage = "زمان آپلود به پایان رسید؛ لطفاً اتصال اینترنت را بررسی کرده و دوباره تلاش کنید"
        val offlineMessage = "اتصال اینترنت برقرار نیست؛ لطفاً شبکه را بررسی کرده و دوباره تلاش کنید"
        val disconnectMessage = "ارتباط هنگام آپلود قطع شد؛ لطفاً دوباره تلاش کنید"

        assertEquals(
            timeoutMessage,
            SocketTimeoutException("timeout").toUserFriendlyUploadError("خطای پیش‌فرض"),
        )
        assertEquals(
            timeoutMessage,
            IOException("wrapped", InterruptedIOException("timeout")).toUserFriendlyUploadError("خطای پیش‌فرض"),
        )
        assertEquals(
            offlineMessage,
            UnknownHostException("Unable to resolve host").toUserFriendlyUploadError("خطای پیش‌فرض"),
        )
        assertEquals(
            disconnectMessage,
            EOFException("unexpected end of stream").toUserFriendlyUploadError("خطای پیش‌فرض"),
        )
        assertEquals(
            "حجم ویدیو بیش از حد مجاز است",
            IOException("حجم ویدیو بیش از حد مجاز است").toUserFriendlyUploadError("خطای پیش‌فرض"),
        )
        assertEquals(
            "خطا در ارسال پست",
            RuntimeException("Unexpected internal failure").toUserFriendlyUploadError("خطا در ارسال پست"),
        )
    }
}
