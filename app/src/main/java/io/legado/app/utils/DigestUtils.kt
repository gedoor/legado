package io.legado.app.utils

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object DigestUtils {

    /**
     * 消息摘要
     * MD2 MD5 SHA-1 SHA-256 SHA-384 SHA-512
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
     * HmacMD5 HmacSHA1 HmacSHA224 HmacSHA256 HmacSHA384 HmacSHA512
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
