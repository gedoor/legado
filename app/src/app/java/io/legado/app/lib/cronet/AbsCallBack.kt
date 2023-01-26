package io.legado.app.lib.cronet

import androidx.annotation.Keep
import io.legado.app.help.http.okHttpClient
import io.legado.app.utils.DebugLog
import io.legado.app.utils.asIOException
import okhttp3.*
import okhttp3.EventListener
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.Buffer
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

@Keep
abstract class AbsCallBack(
    val originalRequest: Request,
    val mCall: Call,
    private val eventListener: EventListener? = null,
    private val responseCallback: Callback? = null
) : UrlRequest.Callback(), AutoCloseable {

    val buffer = Buffer()

    var mResponse: Response

    private var followCount = 0


    @Throws(IOException::class)
    abstract fun waitForDone(urlRequest: UrlRequest): Response

    /**
     * 当发生错误时，通知子类终止阻塞抛出错误
     * @param error
     */
    abstract fun onError(error: IOException)

    /**
     * 请求成功后，通知子类结束阻塞，返回response
     * @param response
     */
    abstract fun onSuccess(response: Response)


    override fun onRedirectReceived(
        request: UrlRequest,
        info: UrlResponseInfo,
        newLocationUrl: String
    ) {
        if (followCount > MAX_FOLLOW_COUNT) {
            request.cancel()
            onError(IOException("Too many redirect"))
            return
        }
        if (mCall.isCanceled()) {
            onError(IOException("Request Canceled"))
            request.cancel()
            return
        }
        followCount += 1
        val client = okHttpClient
        if (originalRequest.url.isHttps && newLocationUrl.startsWith("http://") && client.followSslRedirects) {
            request.followRedirect()
        } else if (!originalRequest.url.isHttps && newLocationUrl.startsWith("https://") && client.followSslRedirects) {
            request.followRedirect()
        } else if (okHttpClient.followRedirects) {
            request.followRedirect()
        } else {
            onError(IOException("Too many redirect"))
            request.cancel()
        }
    }


    override fun onResponseStarted(request: UrlRequest, info: UrlResponseInfo) {
        this.mResponse = responseFromResponse(this.mResponse, info)
        //打印协议，用于调试
        DebugLog.i(javaClass.simpleName, "start[${info.negotiatedProtocol}]${info.url}")
        if (eventListener != null) {
            eventListener.responseHeadersEnd(mCall, this.mResponse)
            eventListener.responseBodyStart(mCall)
        }
        request.read(ByteBuffer.allocateDirect(32 * 1024))
    }


    @Throws(IOException::class)
    override fun onReadCompleted(
        request: UrlRequest,
        info: UrlResponseInfo,
        byteBuffer: ByteBuffer
    ) {


        if (mCall.isCanceled()) {
            request.cancel()
            onError(IOException("Request Canceled"))
        }

        byteBuffer.flip()

        try {
            buffer.write(byteBuffer)
        } catch (e: IOException) {
            DebugLog.e(javaClass.name, "IOException during ByteBuffer read. Details: ", e)
            onError(IOException("IOException during ByteBuffer read. Details:", e))
            return
        }
        byteBuffer.clear()
        request.read(byteBuffer)
    }


    override fun onSucceeded(request: UrlRequest, info: UrlResponseInfo) {
        eventListener?.responseBodyEnd(mCall, info.receivedByteCount)
        val contentType: MediaType? = (this.mResponse.header("content-type")
            ?: "text/plain; charset=\"utf-8\"").toMediaTypeOrNull()
        val responseBody: ResponseBody =
            buffer.asResponseBody(contentType)
        val newRequest = originalRequest.newBuilder().url(info.url).build()
        this.mResponse = this.mResponse.newBuilder().body(responseBody).request(newRequest).build()
        onSuccess(this.mResponse)
        //DebugLog.i(javaClass.simpleName, "end[${info.negotiatedProtocol}]${info.url}")

        eventListener?.callEnd(mCall)
        if (responseCallback != null) {
            try {
                responseCallback.onResponse(mCall, this.mResponse)
            } catch (e: IOException) {
                // Pass?
            }
        }
    }


    //UrlResponseInfo可能为null
    override fun onFailed(request: UrlRequest, info: UrlResponseInfo?, error: CronetException) {
        DebugLog.e(javaClass.name, error.message.toString())
        onError(error.asIOException())
        this.eventListener?.callFailed(mCall, error)
        responseCallback?.onFailure(mCall, error)
    }

    override fun onCanceled(request: UrlRequest?, info: UrlResponseInfo?) {
        super.onCanceled(request, info)
        this.eventListener?.callEnd(mCall)
        //onError(IOException("Cronet Request Canceled"))
    }


    init {
        mResponse = Response.Builder()
            .sentRequestAtMillis(System.currentTimeMillis())
            .request(originalRequest)
            .protocol(Protocol.HTTP_1_0)
            .code(0)
            .message("")
            .build()
    }

    companion object {
        const val MAX_FOLLOW_COUNT = 20
        private fun protocolFromNegotiatedProtocol(responseInfo: UrlResponseInfo): Protocol {
            val negotiatedProtocol = responseInfo.negotiatedProtocol.lowercase(Locale.getDefault())
            return when {
                negotiatedProtocol.contains("h3") -> {
                    return Protocol.QUIC
                }
                negotiatedProtocol.contains("quic") -> {
                    Protocol.QUIC
                }
                negotiatedProtocol.contains("spdy") -> {
                    @Suppress("DEPRECATION")
                    Protocol.SPDY_3
                }
                negotiatedProtocol.contains("h2") -> {
                    Protocol.HTTP_2
                }
                negotiatedProtocol.contains("1.1") -> {
                    Protocol.HTTP_1_1
                }
                else -> {
                    Protocol.HTTP_1_0
                }
            }
        }

        private fun headersFromResponse(responseInfo: UrlResponseInfo): Headers {
            val headers = responseInfo.allHeadersAsList
            return Headers.Builder().apply {
                for ((key, value) in headers) {
                    try {

                        if (key.equals("content-encoding", ignoreCase = true)) {
                            // Strip all content encoding headers as decoding is done handled by cronet
                            continue
                        }
                        add(key, value)
                    } catch (e: Exception) {
                        DebugLog.w(javaClass.name, "Invalid HTTP header/value: $key$value")
                        // Ignore that header
                    }
                }

            }.build()

        }

        private fun responseFromResponse(
            response: Response,
            responseInfo: UrlResponseInfo
        ): Response {
            val protocol = protocolFromNegotiatedProtocol(responseInfo)
            val headers = headersFromResponse(responseInfo)
            return response.newBuilder()
                .receivedResponseAtMillis(System.currentTimeMillis())
                .protocol(protocol)
                .code(responseInfo.httpStatusCode)
                .message(responseInfo.httpStatusText)
                .headers(headers)
                .build()
        }
    }

    override fun close() {
        buffer.clear()
    }
}