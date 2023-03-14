package io.legado.app.help

import cn.hutool.crypto.asymmetric.Sign as HutoolSign
import cn.hutool.crypto.KeyUtil

class Sign(algorithm: String): HutoolSign(algorithm) {

    fun setPrivateKey(key: ByteArray): Sign {
        return setPrivateKey(
            KeyUtil.generatePrivateKey(this.algorithm, key)
        )
    }
    fun setPrivateKey(key: String): Sign = setPrivateKey(key.encodeToByteArray())

    fun setPublicKey(key: ByteArray): Sign {
        return setPublicKey(
            KeyUtil.generatePublicKey(this.algorithm, key)
        )
    }
    fun setPublicKey(key: String): Sign = setPublicKey(key.encodeToByteArray())

}

