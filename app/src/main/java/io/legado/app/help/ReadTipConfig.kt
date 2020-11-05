package io.legado.app.help

import io.legado.app.App
import io.legado.app.R

object ReadTipConfig {
    val tipArray: Array<String> = App.INSTANCE.resources.getStringArray(R.array.read_tip)
    const val none = 0
    const val chapterTitle = 1
    const val time = 2
    const val battery = 3
    const val page = 4
    const val totalProgress = 5
    const val pageAndTotal = 6
    const val bookName = 7

    val tipHeaderLeftStr: String get() = tipArray.getOrElse(tipHeaderLeft) { tipArray[none] }
    val tipHeaderMiddleStr: String get() = tipArray.getOrElse(tipHeaderMiddle) { tipArray[none] }
    val tipHeaderRightStr: String get() = tipArray.getOrElse(tipHeaderRight) { tipArray[none] }
    val tipFooterLeftStr: String get() = tipArray.getOrElse(tipFooterLeft) { tipArray[none] }
    val tipFooterMiddleStr: String get() = tipArray.getOrElse(tipFooterMiddle) { tipArray[none] }
    val tipFooterRightStr: String get() = tipArray.getOrElse(tipFooterRight) { tipArray[none] }

    var tipHeaderLeft: Int
        get() = ReadBookConfig.config.tipHeaderLeft
        set(value) {
            ReadBookConfig.config.tipHeaderLeft = value
        }

    var tipHeaderMiddle: Int
        get() = ReadBookConfig.config.tipHeaderMiddle
        set(value) {
            ReadBookConfig.config.tipHeaderMiddle = value
        }

    var tipHeaderRight: Int
        get() = ReadBookConfig.config.tipHeaderRight
        set(value) {
            ReadBookConfig.config.tipHeaderRight = value
        }

    var tipFooterLeft: Int
        get() = ReadBookConfig.config.tipFooterLeft
        set(value) {
            ReadBookConfig.config.tipFooterLeft = value
        }

    var tipFooterMiddle: Int
        get() = ReadBookConfig.config.tipFooterMiddle
        set(value) {
            ReadBookConfig.config.tipFooterMiddle = value
        }

    var tipFooterRight: Int
        get() = ReadBookConfig.config.tipFooterRight
        set(value) {
            ReadBookConfig.config.tipFooterRight = value
        }

    var hideHeader: Boolean
        get() = ReadBookConfig.config.hideHeader
        set(value) {
            ReadBookConfig.config.hideHeader = value
        }

    var hideFooter: Boolean
        get() = ReadBookConfig.config.hideFooter
        set(value) {
            ReadBookConfig.config.hideFooter = value
        }
}