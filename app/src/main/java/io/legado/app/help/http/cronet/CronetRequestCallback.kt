package io.legado.app.help.http.cronet

import android.os.ConditionVariable
import io.legado.app.help.http.okHttpClient
import okhttp3.*
import okhttp3.EventListener
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.Buffer
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import timber.log.Timber
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

class CronetRequestCallback @JvmOverloads internal constructor(
    private val originalRequest: Request,
    private val mCall: Call,
    eventListener: EventListener? = null,
    responseCallback: Callback? = null
) : UrlRequest.Callback() {

    private val eventListener: EventListener?
    private val responseCallback: Callback?
    private var followCount = 0
    private var mResponse: Response
    private var mException: IOException? = null
    private val mResponseCondition = ConditionVariable()
    private val mBuffer = Buffer()

    @Throws(IOException::class)
    fun waitForDone(urlRequest: UrlRequest): Response {
        //获取okhttp call的完整请求的超时时间
        val timeOutMs: Long = mCall.timeout().timeoutNanos() / 1000000
        if (timeOutMs > 0) {
            mResponseCondition.block(timeOutMs)
        } else {
            mResponseCondition.block()
        }
        //ConditionVariable 正常open或者超时open后，检查urlRequest是否完成
        if (!urlRequest.isDone) {
            urlRequest.cancel()
            mException = IOException("Cronet timeout after wait " + timeOutMs + "ms")
        }

        if (mException != null) {
            throw mException as IOException
        }
        return this.mResponse
    }

    override fun onRedirectReceived(
        request: UrlRequest,
        info: UrlResponseInfo,
        newLocationUrl: String
    ) {
        if (followCount > MAX_FOLLOW_COUNT) {
            request.cancel()
        }
        followCount += 1
        val client = okHttpClient
        if (originalRequest.url.isHttps && newLocationUrl.startsWith("http://") && client.followSslRedirects) {
            request.followRedirect()
        } else if (!originalRequest.url.isHttps && newLocationUrl.startsWith("https://") && client.followSslRedirects) {
            request.followRedirect()
        } else if (client.followRedirects) {
            request.followRedirect()
        } else {
            request.cancel()
        }
    }

    override fun onResponseStarted(request: UrlRequest, info: UrlResponseInfo) {
        this.mResponse = responseFromResponse(this.mResponse, info)
//        用于调试
//        val sb: StringBuilder = StringBuilder(info.url).append("\r\n")
//        sb.append("[Cached:").append(info.wasCached()).append("][StatusCode:")
//            .append(info.httpStatusCode).append("][StatusText:").append(info.httpStatusText)
//            .append("][Protocol:").append(info.negotiatedProtocol).append("][ByteCount:")
//            .append(info.receivedByteCount).append("]\r\n");
//        val httpHeaders=info.allHeadersAsList
//        httpHeaders.forEach { h ->
//            sb.append("[").append(h.key).append("]").append(h.value).append("\r\n");
//        }
//        Log.e("Cronet", sb.toString())
        //打印协议，用于调试
        Timber.i(info.negotiatedProtocol)
        if (eventListener != null) {
            eventListener.responseHeadersEnd(mCall, this.mResponse)
            eventListener.responseBodyStart(mCall)
        }
        request.read(ByteBuffer.allocateDirect(32 * 1024))
    }

    @Throws(Exception::class)
    override fun onReadCompleted(
        request: UrlRequest,
        info: UrlResponseInfo,
        byteBuffer: ByteBuffer
    ) {


        byteBuffer.flip()

        try {
            mBuffer.write(byteBuffer)
        } catch (e: IOException) {
            Timber.i(e, "IOException during ByteBuffer read. Details: ")
            throw e
        }
        byteBuffer.clear()
        request.read(byteBuffer)
    }

    override fun onSucceeded(request: UrlRequest, info: UrlResponseInfo) {
        eventListener?.responseBodyEnd(mCall, info.receivedByteCount)
        val contentType: MediaType? = (this.mResponse.header("content-type")
            ?: "text/plain; charset=\"utf-8\"").toMediaTypeOrNull()
        val responseBody: ResponseBody =
            mBuffer.asResponseBody(contentType)
        val newRequest = originalRequest.newBuilder().url(info.url).build()
        this.mResponse = this.mResponse.newBuilder().body(responseBody).request(newRequest).build()
        mResponseCondition.open()
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
        Timber.e(error.message.toString())
        val msg = error.localizedMessage
        val e = IOException(msg?.substring(msg.indexOf("net::")), error)
        mException = e
        mResponseCondition.open()

        this.eventListener?.callFailed(mCall, e)
        responseCallback?.onFailure(mCall, e)
    }

    override fun onCanceled(request: UrlRequest, info: UrlResponseInfo?) {
        mResponseCondition.open()
        this.eventListener?.callEnd(mCall)


    }

    companion object {
        private const val MAX_FOLLOW_COUNT = 20

        private fun protocolFromNegotiatedProtocol(responseInfo: UrlResponseInfo): Protocol {
            val negotiatedProtocol = responseInfo.negotiatedProtocol.lowercase(Locale.getDefault())
//            Log.e("Cronet", responseInfo.url)
//            Log.e("Cronet", negotiatedProtocol)
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
                        Timber.w("Invalid HTTP header/value: $key$value")
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

    init {
        this.mResponse = Response.Builder()
            .sentRequestAtMillis(System.currentTimeMillis())
            .request(originalRequest)
            .protocol(Protocol.HTTP_1_0)
            .code(0)
            .message("")
            .build()
        this.responseCallback = responseCallback
        this.eventListener = eventListener
    }
}