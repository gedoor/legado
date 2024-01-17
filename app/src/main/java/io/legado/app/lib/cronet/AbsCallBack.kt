package io.legado.app.lib.cronet

import androidx.annotation.Keep
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.http.CookieManager
import io.legado.app.help.http.CookieManager.cookieJarHeader
import io.legado.app.help.http.okHttpClient
import io.legado.app.utils.DebugLog
import io.legado.app.utils.asIOException
import io.legado.app.utils.splitNotBlank
import kotlinx.coroutines.delay
import okhttp3.*
import okhttp3.EventListener
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.asResponseBody
import okhttp3.internal.http.HttpMethod
import okhttp3.internal.http.StatusLine
import okio.Buffer
import okio.Source
import okio.Timeout
import okio.buffer
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import java.io.IOException
import java.net.ProtocolException
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean


@Keep
abstract class AbsCallBack(
    var originalRequest: Request,
    val mCall: Call,
    var readTimeoutMillis: Int,
    private val eventListener: EventListener? = null,
    private val responseCallback: Callback? = null
) : UrlRequest.Callback() {

    var mResponse: Response
    private var followCount = 0
    private var request: UrlRequest? = null
    private var finished = AtomicBoolean(false)
    private val canceled = AtomicBoolean(false)
    private val callbackResults = ArrayBlockingQueue<CallbackResult>(2)
    private val urlResponseInfoChain = arrayListOf<UrlResponseInfo>()
    private var cancelJob: Coroutine<*>? = null
    private var followRedirect = false
    private var enableCookieJar = false
    private var redirectRequest: Request? = null

    init {
        if (readTimeoutMillis == 0) {
            readTimeoutMillis = Int.MAX_VALUE
        }
        if (originalRequest.header(cookieJarHeader) != null) {
            enableCookieJar = true
            originalRequest = originalRequest.newBuilder()
                .removeHeader(cookieJarHeader).build()
        }
    }


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
            onError(IOException("Cronet Request Canceled"))
            request.cancel()
            return
        }
        followCount += 1
        urlResponseInfoChain.add(info)
        val client = okHttpClient
        if (originalRequest.url.isHttps && newLocationUrl.startsWith("http://") && client.followSslRedirects) {
            followRedirect = true
        } else if (!originalRequest.url.isHttps && newLocationUrl.startsWith("https://") && client.followSslRedirects) {
            followRedirect = true
        } else if (okHttpClient.followRedirects) {
            followRedirect = true
        }

        if (!followRedirect) {
            onError(IOException("Too many redirect"))
        } else {
            val response = toResponse(originalRequest, info, urlResponseInfoChain)
            if (enableCookieJar) {
                CookieManager.saveResponse(response)
            }
            redirectRequest = buildRedirectRequest(response, originalRequest.method, newLocationUrl)
        }
        request.cancel()
    }


    override fun onResponseStarted(request: UrlRequest, info: UrlResponseInfo) {
        this.request = request

        val response: Response
        try {
            response = toResponse(originalRequest, info, urlResponseInfoChain, CronetBodySource())
        } catch (e: IOException) {
            request.cancel()
            cancelJob?.cancel()
            onError(e)
            return
        }

        if (enableCookieJar) {
            CookieManager.saveResponse(response)
        }

        mResponse = response
        onSuccess(response)

        //打印协议，用于调试
        val msg = "onResponseStarted[${info.negotiatedProtocol}][${info.httpStatusCode}]${info.url}"
        DebugLog.i(javaClass.simpleName, msg)
        if (eventListener != null) {
            eventListener.responseHeadersEnd(mCall, response)
            eventListener.responseBodyStart(mCall)
        }
        try {
            responseCallback?.onResponse(mCall, response)
        } catch (e: IOException) {
            // Pass?
        }
    }


    @Throws(IOException::class)
    override fun onReadCompleted(
        request: UrlRequest,
        info: UrlResponseInfo,
        byteBuffer: ByteBuffer
    ) {
        callbackResults.add(CallbackResult(CallbackStep.ON_READ_COMPLETED, byteBuffer))
    }


    override fun onSucceeded(request: UrlRequest, info: UrlResponseInfo) {
        callbackResults.add(CallbackResult(CallbackStep.ON_SUCCESS))
        cancelJob?.cancel()
        eventListener?.responseBodyEnd(mCall, info.receivedByteCount)
        //DebugLog.i(javaClass.simpleName, "end[${info.negotiatedProtocol}]${info.url}")

        eventListener?.callEnd(mCall)
    }


    //UrlResponseInfo可能为null
    override fun onFailed(request: UrlRequest, info: UrlResponseInfo?, error: CronetException) {
        callbackResults.add(CallbackResult(CallbackStep.ON_FAILED, null, error))
        cancelJob?.cancel()
        DebugLog.e(javaClass.name, error.message.toString())
        onError(error.asIOException())
        eventListener?.callFailed(mCall, error)
        responseCallback?.onFailure(mCall, error)
    }

    override fun onCanceled(request: UrlRequest?, info: UrlResponseInfo?) {
        if (followRedirect) {
            followRedirect = false
            if (enableCookieJar) {
                val newRequest = CookieManager.loadRequest(redirectRequest!!)
                buildRequest(newRequest, this)?.start()
            } else {
                buildRequest(redirectRequest!!, this)?.start()
            }
            return
        }
        canceled.set(true)
        callbackResults.add(CallbackResult(CallbackStep.ON_CANCELED))
        cancelJob?.cancel()
        //DebugLog.i(javaClass.simpleName, "cancel[${info?.negotiatedProtocol}]${info?.url}")
        eventListener?.callEnd(mCall)
        onError(IOException("Cronet Request Canceled"))
    }

    fun startCheckCancelJob(request: UrlRequest) {
        cancelJob = Coroutine.async {
            while (!mCall.isCanceled()) {
                delay(1000)
            }
            request.cancel()
        }
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
        private val encodingsHandledByCronet = setOf("br", "deflate", "gzip", "x-gzip")

        private fun protocolFromNegotiatedProtocol(responseInfo: UrlResponseInfo): Protocol {
            val negotiatedProtocol = responseInfo.negotiatedProtocol.lowercase(Locale.getDefault())
            return when {
                negotiatedProtocol.contains("h3") -> {
                    Protocol.QUIC
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

        private fun headersFromResponse(
            responseInfo: UrlResponseInfo,
            keepEncodingAffectedHeaders: Boolean
        ): Headers {

            val headers = responseInfo.allHeadersAsList
            return Headers.Builder().apply {
                for ((key, value) in headers) {
                    try {

                        if (!keepEncodingAffectedHeaders
                            && (key.equals("content-encoding", ignoreCase = true)
                                    || key.equals("Content-Length", ignoreCase = true))
                        ) {
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

        @Throws(IOException::class)
        private fun createResponse(
            request: Request,
            responseInfo: UrlResponseInfo,
            bodySource: Source? = null
        ): Response.Builder {
            val protocol = protocolFromNegotiatedProtocol(responseInfo)

            val contentEncodingHeaders =
                responseInfo.allHeaders.getOrDefault("content-encoding", emptyList())
            val contentEncodingItems = contentEncodingHeaders.flatMap {
                it.splitNotBlank(",").toList()
            }
            val keepEncodingAffectedHeaders = contentEncodingItems.isEmpty()
                    || !encodingsHandledByCronet.containsAll(contentEncodingItems)

            val headers = headersFromResponse(responseInfo, keepEncodingAffectedHeaders)
            val contentLength = if (keepEncodingAffectedHeaders) {
                responseInfo.allHeaders["Content-Length"]?.lastOrNull()
            } else null
            val contentType = responseInfo.allHeaders["content-type"]?.lastOrNull()
                ?: "text/plain; charset=\"utf-8\""

            val responseBody = bodySource?.let {
                createResponseBody(
                    request,
                    responseInfo.httpStatusCode,
                    contentType,
                    contentLength,
                    bodySource
                )
            }

            return Response.Builder()
                .request(request)
                .receivedResponseAtMillis(System.currentTimeMillis())
                .protocol(protocol)
                .code(responseInfo.httpStatusCode)
                .message(responseInfo.httpStatusText)
                .headers(headers)
                .body(responseBody)
        }

        private fun buildPriorResponse(
            request: Request,
            redirectResponseInfos: List<UrlResponseInfo>,
        ): Response? {
            var priorResponse: Response? = null
            if (redirectResponseInfos.isNotEmpty()) {
                for (i in redirectResponseInfos.indices) {
                    val url = redirectResponseInfos[i].url
                    val redirectedRequest = request.newBuilder().url(url).build()
                    priorResponse = createResponse(redirectedRequest, redirectResponseInfos[i])
                        .priorResponse(priorResponse)
                        .build()
                }

            }
            return priorResponse
        }

        @Throws(IOException::class)
        private fun createResponseBody(
            request: Request,
            httpStatusCode: Int,
            contentType: String?,
            contentLengthString: String?,
            bodySource: Source
        ): ResponseBody {

            // Ignore content-length header for HEAD requests (consistency with OkHttp)
            val contentLength: Long = if (request.method == "HEAD") {
                0
            } else {
                contentLengthString?.toLongOrNull() ?: -1
            }

            // Check for absence of body in No Content / Reset Content responses (OkHttp consistency)
            if ((httpStatusCode == 204 || httpStatusCode == 205) && contentLength > 0) {
                throw ProtocolException(
                    "HTTP $httpStatusCode had non-zero Content-Length: $contentLengthString"
                )
            }
            return bodySource.buffer()
                .asResponseBody(contentType?.toMediaTypeOrNull(), contentLength)
        }

        private fun buildRedirectRequest(
            userResponse: Response,
            method: String,
            newLocationUrl: String
        ): Request {
            // Most redirects don't include a request body.
            val requestBuilder = userResponse.request.newBuilder()
            if (HttpMethod.permitsRequestBody(method)) {
                val responseCode = userResponse.code
                val maintainBody = HttpMethod.redirectsWithBody(method) ||
                        responseCode == StatusLine.HTTP_PERM_REDIRECT ||
                        responseCode == StatusLine.HTTP_TEMP_REDIRECT
                if (HttpMethod.redirectsToGet(method) && responseCode != StatusLine.HTTP_PERM_REDIRECT && responseCode != StatusLine.HTTP_TEMP_REDIRECT) {
                    requestBuilder.method("GET", null)
                } else {
                    val requestBody = if (maintainBody) userResponse.request.body else null
                    requestBuilder.method(method, requestBody)
                }
                if (!maintainBody) {
                    requestBuilder.removeHeader("Transfer-Encoding")
                    requestBuilder.removeHeader("Content-Length")
                    requestBuilder.removeHeader("Content-Type")
                }
            }

            return requestBuilder.url(newLocationUrl).build()
        }

        private fun toResponse(
            request: Request,
            responseInfo: UrlResponseInfo,
            redirectResponseInfos: List<UrlResponseInfo>,
            bodySource: Source? = null
        ): Response {
            val responseBuilder = createResponse(request, responseInfo, bodySource)
            val newRequest = request.newBuilder().url(responseInfo.url).build()
            return responseBuilder
                .request(newRequest)
                .priorResponse(buildPriorResponse(request, redirectResponseInfos))
                .build()
        }
    }

    inner class CronetBodySource : Source {

        private var buffer = ByteBuffer.allocateDirect(32 * 1024)
        private var closed = false
        private val timeout = readTimeoutMillis.toLong()

        override fun close() {
            cancelJob?.cancel()
            if (closed) {
                return
            }
            closed = true
            if (!finished.get()) {
                request?.cancel()
            }
        }

        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        override fun read(sink: Buffer, byteCount: Long): Long {
            if (canceled.get()) {
                throw IOException("Cronet Request Canceled")
            }

            require(byteCount >= 0L) { "byteCount < 0: $byteCount" }
            check(!closed) { "closed" }

            if (finished.get()) {
                return -1
            }

            if (byteCount < buffer.limit()) {
                buffer.limit(byteCount.toInt())
            }

            request?.read(buffer)

            val result = callbackResults.poll(timeout, TimeUnit.MILLISECONDS)
            if (result == null) {
                request?.cancel()
                throw IOException("Cronet request body read timeout after wait $timeout ms")
            }

            return when (result.callbackStep) {
                CallbackStep.ON_FAILED -> {
                    finished.set(true)
                    buffer = null
                    throw IOException(result.exception)
                }

                CallbackStep.ON_SUCCESS -> {
                    finished.set(true)
                    buffer = null
                    -1
                }

                CallbackStep.ON_CANCELED -> {
                    buffer = null
                    throw IOException("Request Canceled")
                }

                CallbackStep.ON_READ_COMPLETED -> {
                    result.buffer!!.flip()
                    val bytesWritten = sink.write(result.buffer)
                    result.buffer.clear()
                    bytesWritten.toLong()
                }
            }
        }

        override fun timeout(): Timeout {
            return mCall.timeout()
        }

    }
}
