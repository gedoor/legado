package io.legado.app.help.http.parser

import io.legado.app.help.http.text
import okhttp3.Response
import rxhttp.wrapper.annotation.Parser
import rxhttp.wrapper.exception.HttpStatusCodeException

@Parser(name = "Text")
class TextParser(private val encode: String? = null) : rxhttp.wrapper.parse.Parser<String> {

    override fun onParse(response: Response): String {

        val responseBody = response.body ?: throw HttpStatusCodeException(response, "内容为空")
        return responseBody.text(encode)
    }

}