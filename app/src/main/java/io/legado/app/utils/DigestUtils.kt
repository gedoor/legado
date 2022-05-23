package io.legado.app.utils

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object DigestUtils {

    /**
     * 消息摘要
     * 支持MD5 SHA-1 SHA-224 SHA-256 SHA-384 SHA-512
     * https://developer.android.google.cn/reference/java/security/MessageDigest?hl=en
     */
    fun getDigest(
        algorithm: String,
        data: String?
    ): String {
        data ?: return ""
        val bytes = getDigest(algorithm, data.toByteArray())
        return StringUtils.byteToHexString(bytes)
    }

    fun getDigest(
        algorithm: String,
        data: ByteArray
    ): ByteArray {
        lateinit var bytes: ByteArray
        try {
            val messageDigest = MessageDigest.getInstance(algorithm)
            bytes = messageDigest.digest(data)
        } catch (e: NoSuchAlgorithmException) {
            e.printOnDebug()
        }
        return bytes
    }

    /**
     * 散列消息鉴别码
     * 支持DESMAC DESMAC/CFB8 DESedeMAC DESedeMAC/CFB8 DESedeMAC64 DESwithISO9797 HmacMD5 HmacSHA* ISO9797ALG3MAC PBEwithSHA*
     * https://developer.android.google.cn/reference/kotlin/javax/crypto/Mac?hl=en
     */
    fun getHMac(
        algorithm: String,
        key: String,
        data: String?
    ): String {
        data ?: return ""
        val bytes = getHMac(algorithm, key.toByteArray(), data.toByteArray())
        return StringUtils.byteToHexString(bytes)
    }

    fun getHMac(
        algorithm: String,
        key: ByteArray,
        data: ByteArray
    ): ByteArray {
        lateinit var bytes: ByteArray
        try {
            val mac= Mac.getInstance(algorithm)
            val keySpec = SecretKeySpec(key, algorithm)
            mac.init(keySpec)
            mac.update(data)
            bytes = mac.doFinal()
        } catch(e: NoSuchAlgorithmException) {
            e.printOnDebug()
        }
        return bytes
    }

}
