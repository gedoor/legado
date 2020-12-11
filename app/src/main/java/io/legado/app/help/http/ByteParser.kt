package io.legado.app.help.http

import okhttp3.Response
import rxhttp.wrapper.annotation.Parser

@Parser(name = "ByteArray")
class ByteParser : rxhttp.wrapper.parse.Parser<ByteArray> {

    override fun onParse(response: Response): ByteArray {
        return response.body()!!.bytes()
    }

}