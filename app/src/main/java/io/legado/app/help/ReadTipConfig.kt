package io.legado.app.help

import io.legado.app.App
import io.legado.app.R

object ReadTipConfig {
    val tips by lazy {
        App.INSTANCE.resources.getStringArray(R.array.read_tip).toList()
    }
    val headerModes by lazy {
        linkedMapOf(0 to "状态栏显示时隐藏", 1 to "显示", 2 to "隐藏")
    }
    val footerModes by lazy {
        linkedMapOf(0 to "显示", 1 to "隐藏")
    }
    const val none = 0
    const val chapterTitle = 1
    const val time = 2
    const val battery = 3
    const val page = 4
    const val totalProgress = 5
    const val pageAndTotal = 6
    const val bookName = 7
    const val timeBattery = 8

    val tipHeaderLeftStr: String get() = tips.getOrElse(tipHeaderLeft) { tips[none] }
    val tipHeaderMiddleStr: String get() = tips.getOrElse(tipHeaderMiddle) { tips[none] }
    val tipHeaderRightStr: String get() = tips.getOrElse(tipHeaderRight) { tips[none] }
    val tipFooterLeftStr: String get() = tips.getOrElse(tipFooterLeft) { tips[none] }
    val tipFooterMiddleStr: String get() = tips.getOrElse(tipFooterMiddle) { tips[none] }
    val tipFooterRightStr: String get() = tips.getOrElse(tipFooterRight) { tips[none] }

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

    var headerMode: Int
        get() = ReadBookConfig.config.headerMode
        set(value) {
            ReadBookConfig.config.headerMode = value
        }

    var footerMode: Int
        get() = ReadBookConfig.config.footerMode
        set(value) {
            ReadBookConfig.config.footerMode = value
        }

    var tipColor: Int
        get() = ReadBookConfig.config.tipColor
        set(value) {
            ReadBookConfig.config.tipColor = value
        }
}