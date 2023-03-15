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

    private fun <T> cryptoDelegate(
        data: Any,
        func: (Any) -> T
    ): T {
        return when {
            data is ByteArray -> func.invoke(data as ByteArray)
            data is String -> func.invoke(data as String)
            data is InputStream -> func.invoke(data as InputStream)
            else -> throw IllegalArgumentException("Unexpected data input")
        }
    }
    
    fun decrypt(data: Any, keyType: Int): ByteArray? = cryptoDelegate<ByteArray?>(data) { decrypt(it, getKeyType(keyType)) }
    fun decryptStr(data: Any, keyType: Int): String? = cryptoDelegate<String?>(data) { decryptStr(it, getKeyType(keyType)) }
    
    fun encrypt(data: Any, keyType: Int): ByteArray? = cryptoDelegate<ByteArray?>(data) { encrypt(it, getKeyType(keyType)) }
    fun encryptHex(data: Any, keyType: Int): String? = cryptoDelegate<String?>(data) { encryptHex(it, getKeyType(keyType)) }
    fun encryptBase64(data: Any, keyType: Int): String? =cryptoDelegate<String?>(data) { encryptBase64(it, getKeyType(keyType)) }

}