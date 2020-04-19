package io.legado.app.help

import io.legado.app.App
import io.legado.app.R
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.putPrefBoolean
import io.legado.app.utils.putPrefInt

object ReadTipConfig {
    val tipArray: Array<String> = App.INSTANCE.resources.getStringArray(R.array.read_tip)
    const val none = 0
    const val chapterTitle = 1
    const val time = 2
    const val battery = 3
    const val page = 4
    const val totalProgress = 5
    const val pageAndTotal = 6

    val tipHeaderLeftStr: String get() = tipArray.getOrElse(tipHeaderLeft) { tipArray[none] }
    val tipHeaderMiddleStr: String get() = tipArray.getOrElse(tipHeaderMiddle) { tipArray[none] }
    val tipHeaderRightStr: String get() = tipArray.getOrElse(tipHeaderRight) { tipArray[none] }
    val tipFooterLeftStr: String get() = tipArray.getOrElse(tipFooterLeft) { tipArray[none] }
    val tipFooterMiddleStr: String get() = tipArray.getOrElse(tipFooterMiddle) { tipArray[none] }
    val tipFooterRightStr: String get() = tipArray.getOrElse(tipFooterRight) { tipArray[none] }

    var tipHeaderLeft: Int
        get() = App.INSTANCE.getPrefInt("tipHeaderLeft", time)
        set(value) {
            App.INSTANCE.putPrefInt("tipHeaderLeft", value)
        }

    var tipHeaderMiddle: Int
        get() = App.INSTANCE.getPrefInt("tipHeaderMiddle", none)
        set(value) {
            App.INSTANCE.putPrefInt("tipHeaderMiddle", value)
        }

    var tipHeaderRight: Int
        get() = App.INSTANCE.getPrefInt("tipHeaderRight", battery)
        set(value) {
            App.INSTANCE.putPrefInt("tipHeaderRight", value)
        }

    var tipFooterLeft: Int
        get() = App.INSTANCE.getPrefInt("tipFooterLeft", chapterTitle)
        set(value) {
            App.INSTANCE.putPrefInt("tipFooterLeft", value)
        }

    var tipFooterMiddle: Int
        get() = App.INSTANCE.getPrefInt("tipFooterMiddle", none)
        set(value) {
            App.INSTANCE.putPrefInt("tipFooterMiddle", value)
        }

    var tipFooterRight: Int
        get() = App.INSTANCE.getPrefInt("tipFooterRight", pageAndTotal)
        set(value) {
            App.INSTANCE.putPrefInt("tipFooterRight", value)
        }

    var hideHeader: Boolean
        get() = App.INSTANCE.getPrefBoolean("hideHeader", true)
        set(value) {
            App.INSTANCE.putPrefBoolean("hideHeader", value)
        }

    var hideFooter: Boolean
        get() = App.INSTANCE.getPrefBoolean("hideFooter", false)
        set(value) {
            App.INSTANCE.putPrefBoolean("hideFooter", value)
        }
}