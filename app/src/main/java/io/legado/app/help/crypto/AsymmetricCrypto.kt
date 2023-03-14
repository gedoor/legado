package io.legado.app.help

import cn.hutool.crypto.asymmetric.AsymmetricCrypto as HutoolAsymmetricCrypto
import cn.hutool.crypto.asymmetric.KeyType
import cn.hutool.crypto.KeyUtil
import java.io.InputStream

class AsymmetricCrypto(algorithm: String) : HutoolAsymmetricCrypto(algorithm) {

    fun setPrivateKey(key: ByteArray): AsymmetricCrypto {
        return setPrivateKey(
            KeyUtil.generatePrivateKey(this.algorithm, key)
        )
    }
    fun setPrivateKey(key: String): AsymmetricCrypto = setPrivateKey(key.encodeToByteArray())

    fun setPublicKey(key: ByteArray): AsymmetricCrypto {
        return setPublicKey(
            KeyUtil.generatePublicKey(this.algorithm, key)
        )
    }
    fun setPublicKey(key: String): AsymmetricCrypto = setPublicKey(key.encodeToByteArray())

    private fun getKeyType(): KeyType {
        return when {
            this.publicKey != null -> KeyType.PublicKey
            this.privateKey != null -> KeyType.PrivateKey
            else -> KeyType.SecretKey
        }
    }

    private fun <T> cryptoDelegate(
        data: Any,
        func: (Any, KeyType) -> T
    ): T {
        val keyType = getKeyType()
        return when {
            data is ByteArray -> func.invoke(data, keyType)
            data is String -> func.invoke(data, keyType)
            data is InputStream -> func.invoke(data, keyType)
            else -> null
        }
    }
    
    fun decrypt(data: Any): ByteArray? = cryptoDelegate<ByteArray?>(data, decrypt)
    fun decryptStr(data: Any): String? = cryptoDelegate<String?>(data, decryptStr)
    
    fun encrypt(data: Any): ByteArray? = cryptoDelegate<ByteArray?>(data, encrypt)
    fun encryptHex(data: Any): String? = cryptoDelegate<String?>(data, encryptHex)
    fun encryptBase64(data: Any): String? =cryptoDelegate<String?>(data, encryptBase64)

}