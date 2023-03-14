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
        keyType: Int,
        func: (Any, KeyType) -> T
    ): T {
        val keyType = getKeyType(keyType)
        return when {
            data is ByteArray -> func.invoke(data, keyType)
            data is String -> func.invoke(data, keyType)
            data is InputStream -> func.invoke(data, keyType)
            else -> null
        }
    }
    
    fun decrypt(data: Any, keyType: Int): ByteArray? = cryptoDelegate<ByteArray?>(data, keyType, decrypt)
    fun decryptStr(data: Any, keyType: Int): String? = cryptoDelegate<String?>(data, keyType, decryptStr)
    
    fun encrypt(data: Any, keyType: Int): ByteArray? = cryptoDelegate<ByteArray?>(data, keyType, encrypt)
    fun encryptHex(data: Any, keyType: Int): String? = cryptoDelegate<String?>(data, keyType, encryptHex)
    fun encryptBase64(data: Any, keyType: Int): String? =cryptoDelegate<String?>(data, keyType, encryptBase64)

}