package io.legado.app.help.http

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.asResponseBody
import okhttp3.internal.http.promisesBody
import okio.buffer
import okio.source
import java.util.zip.GZIPInputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

object DecompressInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestBuilder = request.newBuilder()

        var transparentDecompress = false
        if (request.header("Accept-Encoding") == null && request.header("Range") == null) {
            transparentDecompress = true
            requestBuilder.header("Accept-Encoding", "gzip, deflate")
        }

        val response = chain.proceed(requestBuilder.build())
        val body = response.body

        if (!transparentDecompress || !response.promisesBody() || body == null) {
            return response
        }

        val encoding = response.header("Content-Encoding")?.lowercase()
        val source = when (encoding) {
            "gzip" -> GZIPInputStream(body.byteStream()).source().buffer()
            "deflate" -> InflaterInputStream(body.byteStream(), Inflater(true)).source().buffer()
            else -> return response
        }

        return response.newBuilder()
            .removeHeader("Content-Encoding")
            .removeHeader("Content-Length")
            .body(source.asResponseBody(body.contentType(), -1))
            .build()
    }
}
