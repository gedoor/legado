package io.legado.app.help.crypto

import cn.hutool.crypto.KeyUtil
import cn.hutool.crypto.asymmetric.Sign as HutoolSign

@Suppress("unused")
class Sign(algorithm: String): HutoolSign(algorithm) {

    fun setPrivateKey(key: ByteArray): Sign {
        setPrivateKey(
            KeyUtil.generatePrivateKey(this.algorithm, key)
        )
        return this
    }

    fun setPrivateKey(key: String): Sign = setPrivateKey(key.encodeToByteArray())

    fun setPublicKey(key: ByteArray): Sign {
        setPublicKey(
            KeyUtil.generatePublicKey(this.algorithm, key)
        )
        return this
    }

    fun setPublicKey(key: String): Sign = setPublicKey(key.encodeToByteArray())

}

