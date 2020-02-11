package io.legado.app.help.http

import io.legado.app.utils.EncodingDetect
import io.legado.app.utils.UTF8BOMFighter
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type
import java.nio.charset.Charset

class EncodeConverter(private val encode: String? = null) : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type?,
        annotations: Array<Annotation>?,
        retrofit: Retrofit?
    ): Converter<ResponseBody, String>? {
        return Converter { value ->
            val responseBytes = UTF8BOMFighter.removeUTF8BOM(value.bytes())
            var charsetName: String? = encode

            charsetName?.let {
                try {
                    return@Converter String(responseBytes, Charset.forName(charsetName))
                } catch (e: Exception) {
                }
            }

            //根据http头判断
            value.contentType()?.charset()?.let {
                return@Converter String(responseBytes, it)
            }

            //根据内容判断
            charsetName = EncodingDetect.getHtmlEncode(responseBytes)
            String(responseBytes, Charset.forName(charsetName))
        }
    }

}
