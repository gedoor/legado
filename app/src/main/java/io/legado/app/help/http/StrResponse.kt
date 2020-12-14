package io.legado.app.help.http

import okhttp3.*
import okhttp3.Response.Builder

/**
 * An HTTP response.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class StrResponse {
    var rawResponse: Response
    var body: String? = null
    var errorBody: ResponseBody? = null

    constructor(rawResponse: Response, body: String?) {
        this.rawResponse = rawResponse
        this.body = body
    }

    constructor(url: String, body: String?) {
        rawResponse = Builder()
            .code(200)
            .message("OK")
            .protocol(Protocol.HTTP_1_1)
            .request(Request.Builder().url(url).build())
            .build()
        this.body = body
    }

    constructor(rawResponse: Response, errorBody: ResponseBody?) {
        this.rawResponse = rawResponse
        this.errorBody = errorBody
    }

    fun raw() = rawResponse

    fun url(): String {
        rawResponse.networkResponse?.let {
            return it.request.url.toString()
        }
        return rawResponse.request.url.toString()
    }

    fun code(): Int {
        return rawResponse.code
    }

    fun message(): String {
        return rawResponse.message
    }

    fun headers(): Headers {
        return rawResponse.headers
    }

    fun isSuccessful(): Boolean = rawResponse.isSuccessful

    fun errorBody(): ResponseBody? {
        return errorBody
    }

    override fun toString(): String {
        return rawResponse.toString()
    }

}