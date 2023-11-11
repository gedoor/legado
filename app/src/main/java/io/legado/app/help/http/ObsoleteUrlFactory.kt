package io.legado.app.help.http

import android.os.Build
import androidx.annotation.RequiresApi
import io.legado.app.help.http.CookieManager.cookieJarHeader
import io.legado.app.help.http.SSLHelper.unsafeTrustManager
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Dispatcher
import okhttp3.Handshake
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.internal.notifyAll
import okhttp3.internal.wait
import okio.Buffer
import okio.BufferedSink
import okio.ByteString.Companion.encode
import okio.Pipe
import okio.Timeout
import okio.buffer
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.InterruptedIOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.MalformedURLException
import java.net.ProtocolException
import java.net.Proxy
import java.net.SocketPermission
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import java.net.URLStreamHandlerFactory
import java.security.AccessControlException
import java.security.Permission
import java.security.Principal
import java.security.cert.Certificate
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.TreeMap
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

/**
 * OkHttp 3.14 dropped support for the long-deprecated OkUrlFactory class, which allows you to use
 * the HttpURLConnection API with OkHttp's implementation. This class does the same thing using only
 * public APIs in OkHttp. It requires OkHttp 3.14 or newer.
 *
 *
 * Rather than pasting this 1100 line gist into your source code, please upgrade to OkHttp's
 * request/response API. Your code will be shorter, easier to read, and you'll be able to use
 * interceptors.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class ObsoleteUrlFactory(private var client: OkHttpClient) : URLStreamHandlerFactory,
    Cloneable {
    fun client(): OkHttpClient {
        return client
    }

    fun setClient(client: OkHttpClient): ObsoleteUrlFactory {
        this.client = client
        return this
    }

    /**
     * Returns a copy of this stream handler factory that includes a shallow copy of the internal
     * [HTTP client][OkHttpClient].
     */
    public override fun clone(): ObsoleteUrlFactory {
        return ObsoleteUrlFactory(client)
    }

    fun open(url: URL): HttpURLConnection {
        return open(url, client.proxy)
    }

    fun open(url: URL, proxy: Proxy?): HttpURLConnection {
        val protocol = url.protocol
        val copy = client.newBuilder()
            .proxy(proxy)
            .build()
        if (protocol == "http") return OkHttpURLConnection(url, copy)
        if (protocol == "https") return OkHttpsURLConnection(url, copy)
        throw IllegalArgumentException("Unexpected protocol: $protocol")
    }

    /**
     * Creates a URLStreamHandler as a [java.net.URL.setURLStreamHandlerFactory].
     *
     *
     * This code configures OkHttp to handle all HTTP and HTTPS connections
     * created with [java.net.URL.openConnection]: <pre>   `OkHttpClient okHttpClient = new OkHttpClient();
     * URL.setURLStreamHandlerFactory(new ObsoleteUrlFactory(okHttpClient));
    `</pre> *
     */
    override fun createURLStreamHandler(protocol: String): URLStreamHandler? {
        return if (protocol != "http" && protocol != "https") null else object :
            URLStreamHandler() {
            override fun openConnection(url: URL): URLConnection {
                return open(url)
            }

            override fun openConnection(url: URL, proxy: Proxy): URLConnection {
                return open(url, proxy)
            }

            override fun getDefaultPort(): Int {
                if ((protocol == "http")) return 80
                if ((protocol == "https")) return 443
                throw AssertionError()
            }
        }
    }

    internal class OkHttpURLConnection(
        url: URL?, // These fields are confined to the application thread that uses HttpURLConnection.
        var client: OkHttpClient
    ) :
        HttpURLConnection(url), Callback {
        private val networkInterceptor: NetworkInterceptor = NetworkInterceptor()
        var requestHeaders: Headers.Builder = Headers.Builder()
        var responseHeaders: Headers? = null
        var executed = false
        var call: Call? = null

        /** Like the superclass field of the same name, but a long and available on all platforms.  */
        //var fixedContentLength = -1L

        // These fields are guarded by lock.
        private val lock = Any()
        private var response: Response? = null
        private var callFailure: Throwable? = null
        var networkResponse: Response? = null
        var connectPending = true
        var proxy: Proxy? = null
        var handshake: Handshake? = null

        @Throws(IOException::class)
        override fun connect() {
            if (executed) return
            val call = buildCall()
            executed = true
            call.enqueue(this)
            synchronized(lock) {
                try {
                    while (connectPending && (response == null) && (callFailure == null)) {
                        lock.wait() // Wait 'til the network interceptor is reached or the call fails.
                    }
                    if (callFailure != null) {
                        throw propagate(callFailure)
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt() // Retain interrupted status.
                    throw InterruptedIOException()
                }
            }
        }

        override fun disconnect() {
            // Calling disconnect() before a connection exists should have no effect.
            if (call == null) return
            networkInterceptor.proceed() // Unblock any waiting async thread.
            call!!.cancel()
        }

        override fun getErrorStream(): InputStream? {
            return try {
                val response = getResponse(true)
                if (hasBody(response) && response.code >= HTTP_BAD_REQUEST) {
                    response.body!!.byteStream()
                } else null
            } catch (e: IOException) {
                null
            }
        }

        @get:Throws(IOException::class)
        val headers: Headers
            get() {
                if (responseHeaders == null) {
                    val response = getResponse(true)
                    val headers = response.headers
                    responseHeaders = headers.newBuilder()
                        .add(SELECTED_PROTOCOL, response.protocol.toString())
                        .add(RESPONSE_SOURCE, responseSourceHeader(response))
                        .build()
                }
                return responseHeaders as Headers
            }

        override fun getHeaderField(position: Int): String? {
            return try {
                val headers = headers
                if (position < 0 || position >= headers.size) null else headers.value(
                    position
                ).encode().string(Charsets.ISO_8859_1)
            } catch (e: IOException) {
                null
            }
        }

        override fun getHeaderField(fieldName: String?): String? {
            return try {
                if (fieldName == null) statusLineToString(getResponse(true)) else headers[fieldName]
            } catch (e: IOException) {
                null
            }
        }

        override fun getHeaderFieldKey(position: Int): String? {
            return try {
                val headers = headers
                if (position < 0 || position >= headers.size) null else headers.name(position)
            } catch (e: IOException) {
                null
            }
        }

        override fun getHeaderFields(): Map<String, List<String>> {
            return try {
                toMultimap(headers, statusLineToString(getResponse(true)))
            } catch (e: IOException) {
                emptyMap()
            }
        }

        override fun getRequestProperties(): Map<String, List<String>> {
            if (connected) {
                throw IllegalStateException(
                    "Cannot access request header fields after connection is set"
                )
            }
            return toMultimap(requestHeaders.build(), null)
        }

        @Throws(IOException::class)
        override fun getInputStream(): InputStream {
            if (!doInput) {
                throw ProtocolException("This protocol does not support input")
            }
            val response = getResponse(false)
            if (response.code >= HTTP_BAD_REQUEST) throw FileNotFoundException(url.toString())
            return response.body!!.byteStream()
        }

        @Throws(IOException::class)
        override fun getOutputStream(): OutputStream {
            val requestBody = buildCall().request().body as OutputStreamRequestBody?
                ?: throw ProtocolException("method does not support a request body: $method")
            if (requestBody is StreamedRequestBody) {
                connect()
                networkInterceptor.proceed()
            }
            if (requestBody.closed) {
                throw ProtocolException("cannot write request body after response has been read")
            }
            return requestBody.outputStream!!
        }

        override fun getPermission(): Permission {
            val url = getURL()
            var hostname = url.host
            var hostPort = if (url.port != -1) url.port else HttpUrl.defaultPort(url.protocol)
            if (usingProxy()) {
                val proxyAddress = client.proxy!!.address() as InetSocketAddress
                hostname = proxyAddress.hostName
                hostPort = proxyAddress.port
            }
            return SocketPermission("$hostname:$hostPort", "connect, resolve")
        }

        override fun getRequestProperty(field: String?): String? {
            return if (field == null) null else requestHeaders[field]
        }

        override fun setConnectTimeout(timeoutMillis: Int) {
            client = client.newBuilder()
                .connectTimeout(timeoutMillis.toLong(), TimeUnit.MILLISECONDS)
                .build()
        }

        override fun setInstanceFollowRedirects(followRedirects: Boolean) {
            client = client.newBuilder()
                .followRedirects(followRedirects)
                .build()
        }

        override fun getInstanceFollowRedirects(): Boolean {
            return client.followRedirects
        }

        override fun getConnectTimeout(): Int {
            return client.connectTimeoutMillis
        }

        override fun setReadTimeout(timeoutMillis: Int) {
            client = client.newBuilder()
                .readTimeout(timeoutMillis.toLong(), TimeUnit.MILLISECONDS)
                .build()
        }

        override fun getReadTimeout(): Int {
            return client.readTimeoutMillis
        }

        @Throws(IOException::class)
        private fun buildCall(): Call {
            if (call != null) {
                return call as Call
            }
            connected = true
            if (doOutput) {
                if (method == "GET") {
                    method = "POST"
                } else if (!permitsRequestBody(method)) {
                    throw ProtocolException("$method does not support writing")
                }
            }
            if (requestHeaders["User-Agent"] == null) {
                requestHeaders.add("User-Agent", defaultUserAgent())
            }
            var requestBody: OutputStreamRequestBody? = null
            if (permitsRequestBody(method)) {
                var contentType: String? = requestHeaders["Content-Type"]
                if (contentType == null) {
                    contentType = "application/x-www-form-urlencoded"
                    requestHeaders.add("Content-Type", contentType)
                }
                val stream = fixedContentLength != -1 || chunkLength > 0
                var contentLength = -1L
                val contentLengthString: String? = requestHeaders["Content-Length"]
                if (fixedContentLength != -1) {
                    contentLength = fixedContentLength.toLong()
                } else if (contentLengthString != null) {
                    contentLength = contentLengthString.toLong()
                }
                requestBody =
                    if (stream) StreamedRequestBody(contentLength) else BufferedRequestBody(
                        contentLength
                    )
                requestBody.timeout!!.timeout(
                    client.writeTimeoutMillis.toLong(),
                    TimeUnit.MILLISECONDS
                )
            }
            val url: HttpUrl
            try {
                url = getURL().toString().toHttpUrl()
            } catch (e: IllegalArgumentException) {
                val malformedUrl = MalformedURLException()
                malformedUrl.initCause(e)
                throw malformedUrl
            }
            val request: Request = Request.Builder()
                .url(url)
                .headers(requestHeaders.build())
                .method(method, requestBody)
                .build()
            val clientBuilder: OkHttpClient.Builder = client.newBuilder()
            clientBuilder.interceptors().clear()
            clientBuilder.interceptors().add(UnexpectedException.INTERCEPTOR)

            clientBuilder.networkInterceptors().clear()
            clientBuilder.networkInterceptors().add(networkInterceptor)
            clientBuilder.addNetworkInterceptor { chain ->
                var request1 = chain.request()
                val enableCookieJar = request1.header(cookieJarHeader) != null

                if (enableCookieJar) {
                    val requestBuilder = request1.newBuilder()
                    requestBuilder.removeHeader(cookieJarHeader)
                    request1 = CookieManager.loadRequest(requestBuilder.build())
                }

                val networkResponse = chain.proceed(request1)

                if (enableCookieJar) {
                    CookieManager.saveResponse(networkResponse)
                }
                networkResponse
            }

            // Use a separate dispatcher so that limits aren't impacted. But use the same executor service!
            clientBuilder.dispatcher(Dispatcher(client.dispatcher.executorService))

            // If we're currently not using caches, make sure the engine's client doesn't have one.
            if (!getUseCaches()) {
                clientBuilder.cache(null)
            }
            return clientBuilder.build().newCall(request).also { call = it }
        }

        @Throws(IOException::class)
        private fun getResponse(networkResponseOnError: Boolean): Response {
            synchronized(lock) {
                if (response != null) return response as Response
                if (callFailure != null) {
                    if (networkResponseOnError && networkResponse != null) return networkResponse as Response
                    throw propagate(callFailure)
                }
            }
            val call = buildCall()
            networkInterceptor.proceed()
            val requestBody = call.request().body as OutputStreamRequestBody?
            if (requestBody != null) requestBody.outputStream!!.close()
            if (executed) {
                synchronized(lock) {
                    try {
                        while (response == null && callFailure == null) {
                            lock.wait() // Wait until the response is returned or the call fails.
                        }
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt() // Retain interrupted status.
                        throw InterruptedIOException()
                    }
                }
            } else {
                executed = true
                try {
                    onResponse(call, call.execute())
                } catch (e: IOException) {
                    onFailure(call, e)
                }
            }
            synchronized(lock) {
                if (callFailure != null) throw propagate(callFailure)
                if (response != null) return response as Response
            }
            throw AssertionError()
        }

        override fun usingProxy(): Boolean {
            if (proxy != null) return true
            val clientProxy = client.proxy
            return clientProxy != null && clientProxy.type() != Proxy.Type.DIRECT
        }

        @Throws(IOException::class)
        override fun getResponseMessage(): String {
            return getResponse(true).message
        }

        @Throws(IOException::class)
        override fun getResponseCode(): Int {
            return getResponse(true).code
        }

        override fun setRequestProperty(field: String?, newValue: String?) {
            if (connected) {
                throw IllegalStateException("Cannot set request property after connection is made")
            }
            if (field == null) {
                throw NullPointerException("field == null")
            }
            if (newValue == null) {
                return
            }
            requestHeaders[field] = newValue
        }

        override fun setIfModifiedSince(newValue: Long) {
            super.setIfModifiedSince(newValue)
            if (ifModifiedSince != 0L) {
                requestHeaders["If-Modified-Since"] = format(Date(ifModifiedSince))
            } else {
                requestHeaders.removeAll("If-Modified-Since")
            }
        }

        override fun addRequestProperty(field: String?, value: String?) {
            if (connected) {
                throw IllegalStateException("Cannot add request property after connection is made")
            }
            if (field == null) {
                throw NullPointerException("field == null")
            }
            if (value == null) {
                return
            }
            requestHeaders.add(field, value)
        }

        @Throws(ProtocolException::class)
        override fun setRequestMethod(method: String) {
            if (!METHODS.contains(method)) {
                throw ProtocolException("Expected one of $METHODS but was $method")
            }
            this.method = method
        }

        override fun setFixedLengthStreamingMode(contentLength: Int) {
            setFixedLengthStreamingMode(contentLength.toLong())
        }

        override fun setFixedLengthStreamingMode(contentLength: Long) {
            if (super.connected) throw IllegalStateException("Already connected")
            if (chunkLength > 0) throw IllegalStateException("Already in chunked mode")
            if (contentLength < 0) throw IllegalArgumentException("contentLength < 0")
            this.fixedContentLength = contentLength.toInt()
            super.fixedContentLength = contentLength.toInt().coerceAtMost(Int.MAX_VALUE)
        }

        override fun onFailure(call: Call, e: IOException) {
            synchronized(lock) {
                callFailure = if ((e is UnexpectedException)) e.cause else e
                lock.notifyAll()
            }
        }

        override fun onResponse(call: Call, response: Response) {
            synchronized(lock) {
                this.response = response
                handshake = response.handshake
                url = response.request.url.toUrl()
                lock.notifyAll()
            }
        }

        internal inner class NetworkInterceptor : Interceptor {
            // Guarded by HttpUrlConnection.this.
            private var proceed = false
            fun proceed() {
                synchronized(lock) {
                    proceed = true
                    lock.notifyAll()
                }
            }

            @Throws(IOException::class)
            override fun intercept(chain: Interceptor.Chain): Response {
                var request: Request = chain.request()
                synchronized(lock) {
                    connectPending = false
                    proxy = chain.connection()!!.route().proxy
                    handshake = chain.connection()!!.handshake()
                    lock.notifyAll()
                    try {
                        while (!proceed) {
                            lock.wait() // Wait until proceed() is called.
                        }
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt() // Retain interrupted status.
                        throw InterruptedIOException()
                    }
                }

                // Try to lock in the Content-Length before transmitting the request body.
                if (request.body is OutputStreamRequestBody) {
                    val requestBody = request.body as OutputStreamRequestBody?
                    request = requestBody!!.prepareToSendRequest(request)
                }
                val response: Response = chain.proceed(request)
                synchronized(lock) {
                    networkResponse = response
                    url = response.request.url.toUrl()
                }
                return response
            }
        }
    }

    internal abstract class OutputStreamRequestBody : RequestBody() {
        var timeout: Timeout? = null
        var expectedContentLength: Long = 0
        var outputStream: OutputStream? = null
        var closed = false
        fun initOutputStream(sink: BufferedSink, expectedContentLength: Long) {
            timeout = sink.timeout()
            this.expectedContentLength = expectedContentLength

            // An output stream that writes to sink. If expectedContentLength is not -1, then this expects
            // exactly that many bytes to be written.
            outputStream = object : OutputStream() {
                private var bytesReceived: Long = 0

                @Throws(IOException::class)
                override fun write(b: Int) {
                    write(byteArrayOf(b.toByte()), 0, 1)
                }

                @Throws(IOException::class)
                override fun write(source: ByteArray, offset: Int, byteCount: Int) {
                    if (closed) throw IOException("closed") // Not IllegalStateException!
                    if (expectedContentLength != -1L && bytesReceived + byteCount > expectedContentLength) {
                        throw ProtocolException(
                            "expected " + expectedContentLength
                                    + " bytes but received " + bytesReceived + byteCount
                        )
                    }
                    bytesReceived += byteCount.toLong()
                    try {
                        sink.write(source, offset, byteCount)
                    } catch (e: InterruptedIOException) {
                        throw SocketTimeoutException(e.message)
                    }
                }

                @Throws(IOException::class)
                override fun flush() {
                    if (closed) return  // Weird, but consistent with historical behavior.
                    sink.flush()
                }

                @Throws(IOException::class)
                override fun close() {
                    closed = true
                    if (expectedContentLength != -1L && bytesReceived < expectedContentLength) {
                        throw ProtocolException(
                            ("expected " + expectedContentLength
                                    + " bytes but received " + bytesReceived)
                        )
                    }
                    sink.close()
                }
            }
        }

        override fun contentLength(): Long {
            return expectedContentLength
        }

        override fun contentType(): MediaType? {
            return null // Let the caller provide this in a regular header.
        }

        @Throws(IOException::class)
        open fun prepareToSendRequest(request: Request): Request {
            return request
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    internal class BufferedRequestBody(expectedContentLength: Long) :
        OutputStreamRequestBody() {
        val buffer = Buffer()
        var contentLength = -1L

        init {
            initOutputStream(buffer, expectedContentLength)
        }

        override fun contentLength(): Long {
            return contentLength
        }

        @Throws(IOException::class)
        override fun prepareToSendRequest(request: Request): Request {
            if (request.header("Content-Length") != null) return request
            outputStream!!.close()
            contentLength = buffer.size
            return request.newBuilder()
                .removeHeader("Transfer-Encoding")
                .header("Content-Length", buffer.size.toString())
                .build()
        }

        override fun writeTo(sink: BufferedSink) {
            buffer.copyTo(sink.buffer, 0, buffer.size)
        }
    }

    internal class StreamedRequestBody(expectedContentLength: Long) :
        OutputStreamRequestBody() {
        private val pipe = Pipe(8192)

        init {
            initOutputStream(pipe.sink.buffer(), expectedContentLength)
        }

        override fun isOneShot(): Boolean {
            return true
        }

        @Throws(IOException::class)
        override fun writeTo(sink: BufferedSink) {
            val buffer = Buffer()
            while (pipe.source.read(buffer, 8192) != -1L) {
                sink.write(buffer, buffer.size)
            }
        }
    }

    internal abstract class DelegatingHttpsURLConnection(private val delegate: HttpURLConnection) :
        HttpsURLConnection(delegate.url) {
        protected abstract fun handshake(): Handshake?
        abstract override fun setHostnameVerifier(hostnameVerifier: HostnameVerifier)
        abstract override fun getHostnameVerifier(): HostnameVerifier
        abstract override fun setSSLSocketFactory(sslSocketFactory: SSLSocketFactory?)
        abstract override fun getSSLSocketFactory(): SSLSocketFactory
        override fun getCipherSuite(): String? {
            val handshake = handshake()
            return handshake?.cipherSuite?.javaName
        }

        override fun getLocalCertificates(): Array<Certificate>? {
            val handshake = handshake() ?: return null
            val result = handshake.localCertificates
            return if (result.isNotEmpty()) result.toTypedArray() else null
        }

        override fun getServerCertificates(): Array<Certificate>? {
            val handshake = handshake() ?: return null
            val result = handshake.peerCertificates
            return if (result.isNotEmpty()) result.toTypedArray() else null
        }

        override fun getPeerPrincipal(): Principal? {
            return handshake()?.peerPrincipal
        }

        override fun getLocalPrincipal(): Principal? {
            return handshake()?.localPrincipal
        }

        @Throws(IOException::class)
        override fun connect() {
            connected = true
            delegate.connect()
        }

        override fun disconnect() {
            delegate.disconnect()
        }

        override fun getErrorStream(): InputStream? {
            return delegate.errorStream
        }

        override fun getRequestMethod(): String {
            return delegate.requestMethod
        }

        @Throws(IOException::class)
        override fun getResponseCode(): Int {
            return delegate.responseCode
        }

        @Throws(IOException::class)
        override fun getResponseMessage(): String? {
            return delegate.responseMessage
        }

        @Throws(ProtocolException::class)
        override fun setRequestMethod(method: String) {
            delegate.requestMethod = method
        }

        override fun usingProxy(): Boolean {
            return delegate.usingProxy()
        }

        override fun getInstanceFollowRedirects(): Boolean {
            return delegate.instanceFollowRedirects
        }

        override fun setInstanceFollowRedirects(followRedirects: Boolean) {
            delegate.instanceFollowRedirects = followRedirects
        }

        override fun getAllowUserInteraction(): Boolean {
            return delegate.allowUserInteraction
        }

        @Throws(IOException::class)
        override fun getContent(): Any {
            return delegate.content
        }

        @Throws(IOException::class)
        override fun getContent(types: Array<Class<*>?>?): Any? {
            return delegate.getContent(types)
        }

        override fun getContentEncoding(): String? {
            return delegate.contentEncoding
        }

        override fun getContentLength(): Int {
            return delegate.contentLength
        }

        // Should only be invoked on Java 8+ or Android API 24+.
        @RequiresApi(Build.VERSION_CODES.N)
        override fun getContentLengthLong(): Long {
            return delegate.contentLengthLong
        }

        override fun getContentType(): String? {
            return delegate.contentType
        }

        override fun getDate(): Long {
            return delegate.date
        }

        override fun getDefaultUseCaches(): Boolean {
            return delegate.defaultUseCaches
        }

        override fun getDoInput(): Boolean {
            return delegate.doInput
        }

        override fun getDoOutput(): Boolean {
            return delegate.doOutput
        }

        override fun getExpiration(): Long {
            return delegate.expiration
        }

        override fun getHeaderField(pos: Int): String? {
            return delegate.getHeaderField(pos)
        }

        override fun getHeaderFields(): Map<String, List<String>> {
            return delegate.headerFields
        }

        override fun getRequestProperties(): Map<String, List<String>> {
            return delegate.requestProperties
        }

        override fun addRequestProperty(field: String, newValue: String) {
            delegate.addRequestProperty(field, newValue)
        }

        override fun getHeaderField(key: String): String? {
            return delegate.getHeaderField(key)
        }

        // Should only be invoked on Java 8+ or Android API 24+.
        @RequiresApi(Build.VERSION_CODES.N)
        override fun getHeaderFieldLong(field: String, defaultValue: Long): Long {
            return delegate.getHeaderFieldLong(field, defaultValue)
        }

        override fun getHeaderFieldDate(field: String, defaultValue: Long): Long {
            return delegate.getHeaderFieldDate(field, defaultValue)
        }

        override fun getHeaderFieldInt(field: String, defaultValue: Int): Int {
            return delegate.getHeaderFieldInt(field, defaultValue)
        }

        override fun getHeaderFieldKey(position: Int): String? {
            return delegate.getHeaderFieldKey(position)
        }

        override fun getIfModifiedSince(): Long {
            return delegate.ifModifiedSince
        }

        @Throws(IOException::class)
        override fun getInputStream(): InputStream {
            return delegate.inputStream
        }

        override fun getLastModified(): Long {
            return delegate.lastModified
        }

        @Throws(IOException::class)
        override fun getOutputStream(): OutputStream {
            return delegate.outputStream
        }

        @Throws(IOException::class)
        override fun getPermission(): Permission {
            return delegate.permission
        }

        override fun getRequestProperty(field: String): String? {
            return delegate.getRequestProperty(field)
        }

        override fun getURL(): URL {
            return delegate.url
        }

        override fun getUseCaches(): Boolean {
            return delegate.useCaches
        }

        override fun setAllowUserInteraction(newValue: Boolean) {
            delegate.allowUserInteraction = newValue
        }

        override fun setDefaultUseCaches(newValue: Boolean) {
            delegate.defaultUseCaches = newValue
        }

        override fun setDoInput(newValue: Boolean) {
            delegate.doInput = newValue
        }

        override fun setDoOutput(newValue: Boolean) {
            delegate.doOutput = newValue
        }

        // Should only be invoked on Java 8+ or Android API 24+.
        override fun setFixedLengthStreamingMode(contentLength: Long) {
            delegate.setFixedLengthStreamingMode(contentLength)
        }

        override fun setIfModifiedSince(newValue: Long) {
            delegate.ifModifiedSince = newValue
        }

        override fun setRequestProperty(field: String, newValue: String) {
            delegate.setRequestProperty(field, newValue)
        }

        override fun setUseCaches(newValue: Boolean) {
            delegate.useCaches = newValue
        }

        override fun setConnectTimeout(timeoutMillis: Int) {
            delegate.connectTimeout = timeoutMillis
        }

        override fun getConnectTimeout(): Int {
            return delegate.connectTimeout
        }

        override fun setReadTimeout(timeoutMillis: Int) {
            delegate.readTimeout = timeoutMillis
        }

        override fun getReadTimeout(): Int {
            return delegate.readTimeout
        }

        override fun toString(): String {
            return delegate.toString()
        }

        override fun setFixedLengthStreamingMode(contentLength: Int) {
            delegate.setFixedLengthStreamingMode(contentLength)
        }

        override fun setChunkedStreamingMode(chunkLength: Int) {
            delegate.setChunkedStreamingMode(chunkLength)
        }
    }

    internal class OkHttpsURLConnection(private val delegate: OkHttpURLConnection) :
        DelegatingHttpsURLConnection(delegate) {
        constructor(url: URL?, client: OkHttpClient) : this(OkHttpURLConnection(url, client))

        override fun handshake(): Handshake? {
            if (delegate.call == null) {
                throw IllegalStateException("Connection has not yet been established")
            }
            return delegate.handshake
        }

        override fun setHostnameVerifier(hostnameVerifier: HostnameVerifier) {
            delegate.client = delegate.client.newBuilder()
                .hostnameVerifier(hostnameVerifier)
                .build()
        }

        override fun getHostnameVerifier(): HostnameVerifier {
            return delegate.client.hostnameVerifier
        }

        override fun setSSLSocketFactory(sslSocketFactory: SSLSocketFactory?) {
            if (sslSocketFactory == null) {
                throw IllegalArgumentException("sslSocketFactory == null")
            }
            // This fails in JDK 9 because OkHttp is unable to extract the trust manager.
            delegate.client = delegate.client.newBuilder()
                .sslSocketFactory(sslSocketFactory, unsafeTrustManager)
                .build()
        }

        override fun getSSLSocketFactory(): SSLSocketFactory {
            return delegate.client.sslSocketFactory
        }
    }

    internal class UnexpectedException(cause: Throwable?) : IOException(cause) {
        companion object {
            val INTERCEPTOR = Interceptor { chain: Interceptor.Chain ->
                try {
                    return@Interceptor chain.proceed(chain.request())
                } catch (e: Error) {
                    throw UnexpectedException(e)
                } catch (e: RuntimeException) {
                    throw UnexpectedException(e)
                }
            }
        }
    }

    companion object {
        const val SELECTED_PROTOCOL = "ObsoleteUrlFactory-Selected-Protocol"
        const val RESPONSE_SOURCE = "ObsoleteUrlFactory-Response-Source"
        val METHODS: Set<String> = LinkedHashSet(
            listOf("OPTIONS", "GET", "HEAD", "POST", "PUT", "DELETE", "TRACE", "PATCH")
        )
        val UTC: TimeZone = TimeZone.getTimeZone("GMT")
        const val HTTP_CONTINUE = 100
        val STANDARD_DATE_FORMAT: ThreadLocal<DateFormat> = ThreadLocal.withInitial {

            // Date format specified by RFC 7231 section 7.1.1.1.
            val rfc1123: DateFormat = SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss 'GMT'",
                Locale.US
            )
            rfc1123.isLenient = false
            rfc1123.timeZone = UTC
            rfc1123
        }
        private val FIELD_NAME_COMPARATOR =
            java.util.Comparator { a: String?, b: String? ->
                // @FindBugsSuppressWarnings("ES_COMPARING_PARAMETER_STRING_WITH_EQ")
                if (a === b) {
                    return@Comparator 0
                } else if (a == null) {
                    return@Comparator -1
                } else if (b == null) {
                    return@Comparator 1
                } else {
                    return@Comparator java.lang.String.CASE_INSENSITIVE_ORDER.compare(a, b)
                }
            }

        @Suppress(
            "RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS",
            "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS"
        )
        fun format(value: Date?): String {
            return STANDARD_DATE_FORMAT.get().format(value)
        }

        fun permitsRequestBody(method: String): Boolean {
            return !((method == "GET") || (method == "HEAD"))
        }

        /** Returns true if the response must have a (possibly 0-length) body. See RFC 7231.  */
        fun hasBody(response: Response): Boolean {
            // HEAD requests never yield a body regardless of the response headers.
            if ((response.request.method == "HEAD")) {
                return false
            }
            val responseCode = response.code
            if (((responseCode < HTTP_CONTINUE || responseCode >= 200)
                        && (responseCode != HttpURLConnection.HTTP_NO_CONTENT
                        ) && (responseCode != HttpURLConnection.HTTP_NOT_MODIFIED))
            ) {
                return true
            }

            // If the Content-Length or Transfer-Encoding headers disagree with the response code, the
            // response is malformed. For best compatibility, we honor the headers.
            return (contentLength(response.headers) != -1L
                    || "chunked".equals(
                response.header("Transfer-Encoding"),
                ignoreCase = true
            ))
        }

        fun contentLength(headers: Headers): Long {
            val s = headers["Content-Length"] ?: return -1
            return try {
                s.toLong()
            } catch (e: NumberFormatException) {
                -1
            }
        }

        fun responseSourceHeader(response: Response): String {
            if (response.networkResponse == null) {
                return if (response.cacheResponse == null) "NONE" else "CACHE " + response.code
            }
            return if (response.cacheResponse == null) "NETWORK " + response.code else "CONDITIONAL_CACHE " + response.networkResponse!!.code
        }

        fun statusLineToString(response: Response): String {
            return ((if (response.protocol == Protocol.HTTP_1_0) "HTTP/1.0" else "HTTP/1.1")
                    + ' ' + response.code
                    + ' ' + response.message)
        }

        fun toHumanReadableAscii(s: String): String {
            var i = 0
            val length = s.length
            var c: Int
            while (i < length) {
                c = s.codePointAt(i)
                if (c > '\u001f'.code && c < '\u007f'.code) {
                    i += Character.charCount(c)
                    continue
                }
                val buffer = Buffer()
                buffer.writeUtf8(s, 0, i)
                buffer.writeUtf8CodePoint('?'.code)
                var j = i + Character.charCount(c)
                while (j < length) {
                    c = s.codePointAt(j)
                    buffer.writeUtf8CodePoint((if (c > '\u001f'.code && c < '\u007f'.code) c else '?') as Int)
                    j += Character.charCount(c)
                }
                return buffer.readUtf8()
            }
            return s
        }

        fun toMultimap(headers: Headers, valueForNullKey: String?): Map<String, List<String>> {
            val result: MutableMap<String?, List<String>> = TreeMap(FIELD_NAME_COMPARATOR)
            var i = 0
            val size = headers.size
            while (i < size) {
                val fieldName = headers.name(i)
                val value = headers.value(i)
                val allValues: MutableList<String> = ArrayList()
                val otherValues = result[fieldName]
                if (otherValues != null) {
                    allValues.addAll(otherValues)
                }
                allValues.add(value)
                result[fieldName] = Collections.unmodifiableList(allValues)
                i++
            }
            if (valueForNullKey != null) {
                result[null] = Collections.unmodifiableList(listOf(valueForNullKey))
            }
            return Collections.unmodifiableMap(result)
        }

        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        fun getSystemProperty(key: String?, defaultValue: String?): String? {
            val value: String?
            try {
                value = System.getProperty(key)
            } catch (ex: AccessControlException) {
                return defaultValue
            }
            return value ?: defaultValue
        }

        fun defaultUserAgent(): String {
            val agent = getSystemProperty("http.agent", null)
            return if (agent != null) toHumanReadableAscii(agent) else "ObsoleteUrlFactory"
        }

        @Throws(IOException::class)
        fun propagate(throwable: Throwable?): IOException {
            if (throwable is IOException) throw throwable
            if (throwable is Error) throw throwable
            if (throwable is RuntimeException) throw throwable
            throw AssertionError()
        }

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val okHttpClient = OkHttpClient()
            URL.setURLStreamHandlerFactory(ObsoleteUrlFactory(okHttpClient))
            val url = URL("https://publicobject.com/helloworld.txt")
            val urlConnection = url.openConnection() as HttpURLConnection
            BufferedReader(
                InputStreamReader(urlConnection.inputStream)
            ).use { reader ->
                var line: String?
                while ((reader.readLine().also { line = it }) != null) {
                    println(line)
                }
            }
        }
    }
}
