package io.legado.app.help.http.parser

import okhttp3.Response
import rxhttp.wrapper.annotation.Parser

@Parser(name = "ByteArray")
class ByteParser : rxhttp.wrapper.parse.Parser<ByteArray> {

    override fun onParse(response: Response): ByteArray {
        return response.body!!.bytes()
    }

}