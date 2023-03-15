package io.legado.app.help.crypto

import cn.hutool.crypto.asymmetric.AsymmetricCrypto as HutoolAsymmetricCrypto
import cn.hutool.crypto.asymmetric.KeyType
import cn.hutool.crypto.KeyUtil
import java.io.InputStream

class AsymmetricCrypto(algorithm: String) : HutoolAsymmetricCrypto(algorithm) {

    fun setPrivateKey(key: ByteArray): AsymmetricCrypto {
        setPrivateKey(
            KeyUtil.generatePrivateKey(this.algorithm, key)
        )
        return this
    }
    fun setPrivateKey(key: String): AsymmetricCrypto = setPrivateKey(key.encodeToByteArray())

    fun setPublicKey(key: ByteArray): AsymmetricCrypto {
        setPublicKey(
            KeyUtil.generatePublicKey(this.algorithm, key)
        )
        return this
    }
    fun setPublicKey(key: String): AsymmetricCrypto = setPublicKey(key.encodeToByteArray())

    private fun getKeyType(keyType: Int): KeyType {
        return when {
            keyType == 1 -> KeyType.PublicKey
            keyType == 2 -> KeyType.PrivateKey
            else -> throw IllegalArgumentException("Unexpected keyType:\n 1 -> use publicKey\n 2 -> use PrivateKey")
        }
    }

    fun decrypt(data: Any, keyType: Int): ByteArray = when {
        data is ByteArray -> decrypt(data as ByteArray, getKeyType(keyType))
        data is String -> decrypt(data as String, getKeyType(keyType))
        data is InputStream -> decrypt(data as InputStream, getKeyType(keyType))
        else -> throw IllegalArgumentException("Unexpected data input")
    }
    fun decryptStr(data: Any, keyType: Int): String = when {
        data is ByteArray -> decryptStr(data as ByteArray, getKeyType(keyType))
        data is String -> decryptStr(data as String, getKeyType(keyType))
        data is InputStream -> decryptStr(data as InputStream, getKeyType(keyType))
        else -> throw IllegalArgumentException("Unexpected data input")
    }

    fun encrypt(data: Any, keyType: Int): ByteArray = when {
        data is ByteArray -> encrypt(data as ByteArray, getKeyType(keyType))
        data is String -> encrypt(data as String, getKeyType(keyType))
        data is InputStream -> encrypt(data as InputStream, getKeyType(keyType))
        else -> throw IllegalArgumentException("Unexpected data input")
    }
    fun encryptHex(data: Any, keyType: Int): String = when {
        data is ByteArray -> encryptHex(data as ByteArray, getKeyType(keyType))
        data is String -> encryptHex(data as String, getKeyType(keyType))
        data is InputStream -> encryptHex(data as InputStream, getKeyType(keyType))
        else -> throw IllegalArgumentException("Unexpected data input")
    }
    fun encryptBase64(data: Any, keyType: Int): String = when {
        data is ByteArray -> encryptBase64(data as ByteArray, getKeyType(keyType))
        data is String -> encryptBase64(data as String, getKeyType(keyType))
        data is InputStream -> encryptBase64(data as InputStream, getKeyType(keyType))
        else -> throw IllegalArgumentException("Unexpected data input")
    }


}