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

    private fun getKeyType(usePublicKey: Boolean): KeyType {
        return when {
            usePublicKey == true -> KeyType.PublicKey
            else -> KeyType.PrivateKey
        }
    }

fun decrypt(data: ByteArray, usePublicKey: Boolean): ByteArray = decrypt(data, getKeyType(usePublicKey))
    fun decrypt(data: String, usePublicKey: Boolean): ByteArray = decrypt(data, getKeyType(usePublicKeye))
    fun decrypt(data: InputStream, usePublicKey: Boolean): ByteArray = decrypt(data, getKeyType(usePublicKey))

    fun decryptStr(data: ByteArray, usePublicKey: Boolean): String = decryptStr(data, getKeyType(usePublicKey))
    fun decryptStr(data: String, usePublicKey: Boolean): String = decryptStr(data, getKeyType(usePublicKey))
    fun decryptStr(data: InputStream, usePublicKey: Boolean): String = decryptStr(data, getKeyType(usePublicKey))
    
    fun encrypt(data: ByteArray, usePublicKey: Boolean): ByteArray = encrypt(data, getKeyType(usePublicKey))
    fun encrypt(data: String, usePublicKey: Boolean): ByteArray = encrypt(data, getKeyType(usePublicKey))
    fun encrypt(data: InputStream, usePublicKey: Boolean): ByteArray = encrypt(data, getKeyType(usePublicKey))

    fun encryptHex(data: ByteArray, usePublicKey: Boolean): String = encryptHex(data, getKeyType(usePublicKey))
    fun encryptHex(data: String, usePublicKey: Boolean): String = encryptHex(data, getKeyType(usePublicKey))
    fun encryptHex(data: InputStream, usePublicKey: Boolean): String = encryptHex(data, getKeyType(usePublicKey))

    fun encryptBase64(data: ByteArray, usePublicKey: Boolean): String =encryptBase64(data, getKeyType(usePublicKey))
    fun encryptBase64(data: String, usePublicKey: Boolean): String =encryptBase64(data, getKeyType(usePublicKey))
    fun encryptBase64(data: InputStream, usePublicKey: Boolean): String =encryptBase64(data, getKeyType(usePublicKey))



}