package io.legado.app.utils

import com.github.liuyueyi.quick.transfer.ChineseUtils
import com.github.liuyueyi.quick.transfer.constants.TransType
import com.github.liuyueyi.quick.transfer.dictionary.BasicDictionary
import com.github.liuyueyi.quick.transfer.dictionary.DictionaryContainer

object ChineseUtils {

    private var fixed = false

    fun s2t(content: String): String {
        return ChineseUtils.s2t(content)
    }

    fun t2s(content: String): String {
        if (!fixed) {
            fixed = true
            fixT2sDict()
        }
        return ChineseUtils.t2s(content)
    }

    fun preLoad(async: Boolean, vararg transType: TransType) {
        ChineseUtils.preLoad(async, *transType)
    }

    fun unLoad(vararg transType: TransType) {
        ChineseUtils.unLoad(*transType)
    }

    fun fixT2sDict() {
        val dict = DictionaryContainer.getInstance().getDictionary(TransType.TRADITIONAL_TO_SIMPLE)
        dict.run {
            remove("劈")
            remove("脊")
            remove("支援")
            remove("沈默")
            remove("類比")
            remove("模擬")
            remove("划槳")
            remove("列根")
            remove("路易斯")
            remove("非同步")
            remove("出租车")
            remove("周杰倫")
        }
    }

    fun BasicDictionary.remove(key: String) {
        if (key.length == 1) {
            chars.remove(key[0])
        } else {
            dict.remove(key)
        }
    }

}
