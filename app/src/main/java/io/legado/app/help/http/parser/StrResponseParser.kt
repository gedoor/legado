package io.legado.app.help.http.parser

import io.legado.app.help.http.StrResponse
import io.legado.app.help.http.text
import okhttp3.Response
import rxhttp.wrapper.annotation.Parser
import rxhttp.wrapper.exception.HttpStatusCodeException

@Parser(name = "StrResponse")
class StrResponseParser(private val encode: String? = null) :
    rxhttp.wrapper.parse.Parser<StrResponse> {

    override fun onParse(response: Response): StrResponse {
        val body = response.body?.text(encode)
            ?: throw HttpStatusCodeException(response, "内容为空")
        return StrResponse(response, body)
    }

}