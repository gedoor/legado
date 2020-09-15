package io.legado.app.utils

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * 将字符串转化为MD5
 */
@Suppress("unused")
object MD5Utils {

    fun md5Encode(str: String?): String {
        if (str == null) return ""
        var reStr = ""
        try {
            val md5: MessageDigest = MessageDigest.getInstance("MD5")
            val bytes: ByteArray = md5.digest(str.toByteArray())
            val stringBuffer: StringBuilder = StringBuilder()
            for (b in bytes) {
                val bt: Int = b.toInt() and 0xff
                if (bt < 16) {
                    stringBuffer.append(0)
                }
                stringBuffer.append(Integer.toHexString(bt))
            }
            reStr = stringBuffer.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }

        return reStr
    }

    fun md5Encode16(str: String): String {
        var reStr = md5Encode(str)
        reStr = reStr.substring(8, 24)
        return reStr
    }
}
