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

fun decrypt(data: ByteArray, keyType: Int): ByteArray? = decrypt(data, getKeyType(keyType))
    fun decrypt(data: String, keyType: Int): ByteArray? = decrypt(data, getKeyType(keyType))
    fun decrypt(data: InputStream, keyType: Int): ByteArray? = decrypt(data, getKeyType(keyType))

    fun decryptStr(data: ByteArray, keyType: Int): String? = decryptStr(data, getKeyType(keyType))
    fun decryptStr(data: String, keyType: Int): String? = decryptStr(data, getKeyType(keyType))
    fun decryptStr(data: InputStream, keyType: Int): String? = decryptStr(data, getKeyType(keyType))
    
    fun encrypt(data: ByteArray, keyType: Int): ByteArray? = encrypt(data, getKeyType(keyType))
    fun encrypt(data: String, keyType: Int): ByteArray? = encrypt(data, getKeyType(keyType))
    fun encrypt(data: InputStream, keyType: Int): ByteArray? = encrypt(data, getKeyType(keyType))

    fun encryptHex(data: ByteArray, keyType: Int): String? = encryptHex(data, getKeyType(keyType))
    fun encryptHex(data: String, keyType: Int): String? = encryptHex(data, getKeyType(keyType))
    fun encryptHex(data: InputStream, keyType: Int): String? = encryptHex(data, getKeyType(keyType))

    fun encryptBase64(data: ByteArray, keyType: Int): String? =encryptBase64(data, getKeyType(keyType))
    fun encryptBase64(data: String, keyType: Int): String? =encryptBase64(data, getKeyType(keyType))
    fun encryptBase64(data: InputStream, keyType: Int): String? =encryptBase64(data, getKeyType(keyType))



}