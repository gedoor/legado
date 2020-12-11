package io.legado.app.help.http

import okhttp3.*
import okhttp3.Response.Builder
import java.util.*

/**
 * An HTTP response.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class StrResponse private constructor(
    val rawResponse: Response,
    val body: String?,
    val errorBody: ResponseBody?
) {
    val raw get() = rawResponse

    val url: String
        get() {
            raw.networkResponse?.let {
                return it.request.url.toString()
            }
            return raw.request.url.toString()
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

    val isSuccessful: Boolean
        get() = rawResponse.isSuccessful

    fun errorBody(): ResponseBody? {
        return errorBody
    }

    override fun toString(): String {
        return rawResponse.toString()
    }

    companion object {
        fun success(code: Int, body: String): StrResponse {
            require(!(code < 200 || code >= 300)) { "code < 200 or >= 300: $code" }
            return success(
                body,
                Builder() //
                    .code(code)
                    .message("Response.success()")
                    .protocol(Protocol.HTTP_1_1)
                    .request(Request.Builder().url("http://localhost/").build())
                    .build()
            )
        }

        fun success(body: String, headers: Headers): StrResponse {
            return success(
                body,
                Builder() //
                    .code(200)
                    .message("OK")
                    .protocol(Protocol.HTTP_1_1)
                    .headers(headers)
                    .request(Request.Builder().url("http://localhost/").build())
                    .build()
            )
        }

        fun success(body: String, url: String): StrResponse {
            return success(
                body,
                Builder() //
                    .code(200)
                    .message("OK")
                    .protocol(Protocol.HTTP_1_1)
                    .request(Request.Builder().url(url).build())
                    .build()
            )
        }

        @JvmOverloads
        fun success(
            body: String?, rawResponse: Response =
                Builder() //
                    .code(200)
                    .message("OK")
                    .protocol(Protocol.HTTP_1_1)
                    .request(Request.Builder().url("http://localhost/").build())
                    .build()
        ): StrResponse {
            Objects.requireNonNull(rawResponse, "rawResponse == null")
            require(rawResponse.isSuccessful) { "rawResponse must be successful response" }
            return StrResponse(rawResponse, body, null)
        }

        fun error(code: Int, body: ResponseBody?): StrResponse {
            Objects.requireNonNull(body, "body == null")
            require(code >= 400) { "code < 400: $code" }
            return error(
                body,
                Builder() //
                    .code(code)
                    .message("Response.error()")
                    .protocol(Protocol.HTTP_1_1)
                    .request(Request.Builder().url("http://localhost/").build())
                    .build()
            )
        }

        fun error(body: ResponseBody?, rawResponse: Response): StrResponse {
            Objects.requireNonNull(body, "body == null")
            Objects.requireNonNull(rawResponse, "rawResponse == null")
            require(!rawResponse.isSuccessful) { "rawResponse should not be successful response" }
            return StrResponse(rawResponse, null, body)
        }
    }
}