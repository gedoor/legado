package io.legado.app.help

import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.utils.EncoderUtils
import io.legado.app.utils.MD5Utils
import io.legado.app.utils.StringUtils
import java.util.regex.Pattern


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
    fun base64Decoder(str: String): String {
        return EncoderUtils.base64Decoder(str)
    }

    /**
     * 章节数转数字
     */
    fun toNumChapter(s: String?): String? {
        if (s == null) {
            return null
        }
        val pattern = Pattern.compile("(第)(.+?)(章)")
        val matcher = pattern.matcher(s)
        return if (matcher.find()) {
            matcher.group(1) + StringUtils.stringToInt(matcher.group(2)) + matcher.group(3)
        } else {
            s
        }
    }

    fun strToMd5By32(str: String?): String? {
        return MD5Utils.strToMd5By32(str)
    }

    fun strToMd5By16(str: String?): String? {
        return MD5Utils.strToMd5By16(str)
    }

}
