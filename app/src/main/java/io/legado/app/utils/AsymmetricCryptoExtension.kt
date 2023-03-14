package io.legado.app.utils

import cn.hutool.crypto.asymmetric.*
import cn.hutool.crypto.KeyUtil
import java.io.InputStream

fun AsymmetricCrypto.decrypt(data: Any, keyType: Int): ByteArray? {
    return when {
        data is ByteArray -> decrypt(data, KeyType(keyType))
        data is String -> decrypt(data, KeyType(keyType))
        data is InputStream -> decrypt(data, KeyType(keyType))
        else -> null
    }
}

fun AsymmetricCrypto.decryptStr(data: Any, keyType: Int): String? {
    return when {
        data is ByteArray -> decryptStr(data, KeyType(keyType))
        data is String -> decryptStr(data, KeyType(keyType))
        data is InputStream -> decryptStr(data, KeyType(keyType))
        else -> null
    }
}

fun AsymmetricCrypto.encrypt(data: Any, keyType: Int): ByteArray? {
    return when {
        data is ByteArray -> encrypt(data, KeyType(keyType))
        data is String -> encrypt(data, KeyType(keyType))
        data is InputStream -> encrypt(data, KeyType(keyType))
        else -> null
    }
}
fun AsymmetricCrypto.encryptBase64(data: Any, keyType: Int): String? {
    return when {
        data is ByteArray -> encryptBase64(data, KeyType(keyType))
        data is String -> encryptBase64(data, KeyType(keyType))
        data is InputStream -> encryptBase64(data, KeyType(keyType))
        else -> null
    }
}
fun AsymmetricCrypto.encryptHex(data: Any, keyType: Int): String? {
    return when {
        data is ByteArray -> encryptHex(data, KeyType(keyType))
        data is String -> encryptHex(data, KeyType(keyType))
        data is InputStream -> encryptHex(data, KeyType(keyType))
        else -> null
    }
}

fun AsymmetricCrypto.setPrivateKey(privateKey: ByteArray): AsymmetricCrypto {
    return setPrivateKey(KeyUtil.generatePrivateKey(this.algorithm, privateKey))
}
fun AsymmetricCrypto.setPrivateKey(privateKey: String): AsymmetricCrypto = setPrivateKey(privateKey.encodeToByteArray())

fun AsymmetricCrypto.setPublicKey(publicKey: ByteArray): AsymmetricCrypto {
    return setPublicKey(KeyUtil.generatePublicKey(this.algorithm, publicKey))
}
fun AsymmetricCrypto.setPublicKey(publicKey: String): AsymmetricCrypto = setPublicKey(publicKey.encodeToByteArray())

fun Sign.setPrivateKey(privateKey: ByteArray): Sign {
    return setPrivateKey(KeyUtil.generatePrivateKey(this.algorithm, privateKey))
}

fun Sign.setPrivateKey(privateKey: String): Sign = setPrivateKey(privateKey.encodeToByteArray())

fun Sign.setPublicKey(publicKey: ByteArray): Sign {
    return setPublicKey(KeyUtil.generatePublicKey(this.algorithm, publicKey))
}

fun Sign.setPublicKey(publicKey: String): Sign = setPublicKey(publicKey.encodeToByteArray())

