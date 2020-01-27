package io.legado.app.help

import io.legado.app.App
import io.legado.app.constant.PreferKey
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.putPrefBoolean
import io.legado.app.utils.putPrefInt

object AppConfig {

    var isNightTheme: Boolean
        get() = App.INSTANCE.getPrefBoolean("isNightTheme")
        set(value) {
            App.INSTANCE.putPrefBoolean("isNightTheme", value)
        }

    var isTransparentStatusBar: Boolean
        get() = App.INSTANCE.getPrefBoolean("transparentStatusBar")
        set(value) {
            App.INSTANCE.putPrefBoolean("transparentStatusBar", value)
        }

    var isShowRSS: Boolean
        get() = App.INSTANCE.getPrefBoolean(PreferKey.showRss)
        set(value) {
            App.INSTANCE.putPrefBoolean(PreferKey.showRss, value)
        }

    var threadCount: Int
        get() = App.INSTANCE.getPrefInt(PreferKey.threadCount)
        set(value) {
            App.INSTANCE.putPrefInt(PreferKey.threadCount, value)
        }

    var autoDarkMode: Boolean
        get() = App.INSTANCE.getPrefBoolean(PreferKey.autoDarkMode)
        set(value) {
            App.INSTANCE.putPrefBoolean(PreferKey.autoDarkMode, value)
        }

    val isEInkMode: Boolean
        get() = App.INSTANCE.getPrefBoolean("isEInkMode")
}
