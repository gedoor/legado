https://github.com/gedoor/legado/pull/2880

非对称加密一般只能知道其中一个密钥，而RhinoJs调用java方法不能传入null和KeyType, 因此提供以下重载函数
```kotlin
fun setPublicKey(key: ByteArray): T
fun setPublicKey(key: String): T
fun setPrivateKey(key: ByteArray): T
fun setPrivateKey(key: String): T

fun decrypt(data: Any, usePublicKey: Boolean? = true): ByteArray?
fun decryptStr(data: Any, usePublicKey: Boolean? = true): String?

fun encrypt(data: Any, usePublicKey: Boolean? = true): ByteArray?
fun encryptHex(data: Any, usePublicKey: Boolean? = true): String?
fun encryptBase64(data: Any, usePublicKey: Boolean? = true): String?
```