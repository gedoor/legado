package io.legado.app.help

import io.legado.app.App
import io.legado.app.R
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.putPrefInt

object ReadTipConfig {
    val tipArray = App.INSTANCE.resources.getStringArray(R.array.read_tip)
    const val none = 0
    const val chapterTitle = 1
    const val time = 2
    const val battery = 3
    const val page = 4
    const val totalProgress = 5

    val tipHeaderLeftStr: String = tipArray[tipHeaderLeft]
    val tipHeaderMiddleStr: String = tipArray[tipHeaderMiddle]
    val tipHeaderRightStr: String = tipArray[tipHeaderRight]
    val tipFooterLeftStr: String = tipArray[tipFooterLeft]
    val tipFooterMiddleStr: String = tipArray[tipFooterMiddle]
    val tipFooterRightStr: String = tipArray[tipFooterRight]

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
        get() = App.INSTANCE.getPrefInt("tipFooterMiddle", totalProgress)
        set(value) {
            App.INSTANCE.putPrefInt("tipFooterMiddle", value)
        }

    var tipFooterRight: Int
        get() = App.INSTANCE.getPrefInt("tipFooterRight", page)
        set(value) {
            App.INSTANCE.putPrefInt("tipFooterRight", value)
        }
}