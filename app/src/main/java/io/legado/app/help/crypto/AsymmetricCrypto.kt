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
            else -> KeyType.SecretKey
        }
    }

    fun decrypt(data: Any, keyType: Int): ByteArray = when {
        data is ByteArray -> decrypt(data, getKeyType(keyType))
        data is String -> decrypt(data, getKeyType(keyType))
        data is InputStream -> decrypt(data, getKeyType(keyType))
        else -> throw IllegalArgumentException("Unexpected data input")
    }
    fun decryptStr(data: Any, keyType: Int): String = when {
        data is ByteArray -> decryptStr(data, getKeyType(keyType))
        data is String -> decryptStr(data, getKeyType(keyType))
        data is InputStream -> decryptStr(data, getKeyType(keyType))
        else -> throw IllegalArgumentException("Unexpected data input")
    }

    fun encrypt(data: Any, keyType: Int): ByteArray = when {
        data is ByteArray -> encrypt(data, getKeyType(keyType))
        data is String -> encrypt(data, getKeyType(keyType))
        data is InputStream -> encrypt(data, getKeyType(keyType))
        else -> throw IllegalArgumentException("Unexpected data input")
    }
    fun encryptHex(data: Any, keyType: Int): String = when {
        data is ByteArray -> encryptHex(data, getKeyType(keyType))
        data is String -> encryptHex(data, getKeyType(keyType))
        data is InputStream -> encryptHex(data, getKeyType(keyType))
        else -> throw IllegalArgumentException("Unexpected data input")
    }
    fun encryptBase64(data: Any, keyType: Int): String = when {
        data is ByteArray -> encryptBase64(data, getKeyType(keyType))
        data is String -> encryptBase64(data, getKeyType(keyType))
        data is InputStream -> encryptBase64(data, getKeyType(keyType))
        else -> throw IllegalArgumentException("Unexpected data input")
    }


}