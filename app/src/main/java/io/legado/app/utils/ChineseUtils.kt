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
            remove("劈", "脊", "槃")
            remove("支援", "沈默", "類比", "模擬", "划槳", "列根", "先進", "雪梨", "雪糕")
            remove("零錢", "零钱", "離線", "碟片", "模組", "桌球", "案頭", "機車", "電漿")
            remove("鳳梨", "魔戒", "載入")
            remove("路易斯", "非同步", "出租车", "周杰倫", "马铃薯", "馬鈴薯", "機械人", "電單車")
            remove("電扶梯", "音效卡", "飆車族", "點陣圖", "個入球", "顆進球")
            remove("魔獸紀元", "高空彈跳", "铁达尼号")
            remove("魔鬼終結者", "純文字檔案")
        }
    }

    fun BasicDictionary.remove(vararg keys: String) {
        for (key in keys) {
            if (key.length == 1) {
                chars.remove(key[0])
            } else {
                dict.remove(key)
            }
        }
    }

}
