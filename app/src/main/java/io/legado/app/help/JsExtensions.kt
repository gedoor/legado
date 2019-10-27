package io.legado.app.help

import android.util.Base64
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.utils.EncoderUtils
import io.legado.app.utils.MD5Utils


@Suppress("unused")
object JsExtensions {

    /**
     * js实现跨域访问,不能删
     */
    fun ajax(urlStr: String): String? {
        return try {
            val analyzeUrl = AnalyzeUrl(urlStr, null, null, null, null, null)
            val call = analyzeUrl.getResponse()
            val response = call.execute()
            response.body()
        } catch (e: Exception) {
            e.localizedMessage
        }
    }

    /**
     * js实现解码,不能删
     */
    fun base64Decode(str: String): String {
        return EncoderUtils.base64Decode(str)
    }

    fun base64Encode(str: String): String? {
        return EncoderUtils.base64Encode(str)
    }

    fun base64Encode(str: String, flags: Int = Base64.NO_WRAP): String? {
        return EncoderUtils.base64Encode(str, flags)
    }

    fun md5Encode(str: String): String? {
        return MD5Utils.md5Encode(str)
    }

    fun md5Encode16(str: String): String? {
        return MD5Utils.md5Encode16(str)
    }

}
