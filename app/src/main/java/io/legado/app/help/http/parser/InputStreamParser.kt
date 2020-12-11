package io.legado.app.help.http.parser

import okhttp3.Response
import rxhttp.wrapper.annotation.Parser
import java.io.InputStream

@Parser(name = "InputStream")
class InputStreamParser : rxhttp.wrapper.parse.Parser<InputStream> {

    override fun onParse(response: Response): InputStream {
        return response.body!!.byteStream()
    }

}