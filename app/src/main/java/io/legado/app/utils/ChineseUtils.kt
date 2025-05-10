package io.legado.app.utils

import com.github.liuyueyi.quick.transfer.ChineseUtils
import com.github.liuyueyi.quick.transfer.constants.TransType

object ChineseUtils {

    private var fixed = false

    fun s2t(content: String): String {
        return ChineseUtils.s2t(content)
    }

    fun t2s(content: String): String {
        if (!fixed) {
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
        fixed = true
        val excludeList = listOf(
            "槃",
            "划槳", "列根", "雪梨", "雪糕", "多士", "起司", "芝士",
            "零錢", "零钱", "離線", "碟片", "模組", "桌球", "案頭", "機車", "電漿",
            "鳳梨", "魔戒", "載入", "菲林", "整合", "變數",
            "路易斯", "非同步", "出租车", "周杰倫", "马铃薯", "馬鈴薯", "機械人", "電單車",
            "電扶梯", "音效卡", "飆車族", "點陣圖", "個入球", "顆進球",
            "魔獸紀元", "高空彈跳", "铁达尼号",
            "魔鬼終結者", "純文字檔案"
        )
        ChineseUtils.loadExcludeDict(TransType.TRADITIONAL_TO_SIMPLE, excludeList)
    }

}
