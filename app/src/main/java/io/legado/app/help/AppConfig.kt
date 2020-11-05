package io.legado.app.help

import android.annotation.SuppressLint
import android.content.Context
import io.legado.app.App
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.utils.*

@Suppress("MemberVisibilityCanBePrivate")
object AppConfig {
    var isEInkMode: Boolean = false
    val isGooglePlay: Boolean
    val isCoolApk: Boolean
    var replaceEnableDefault: Boolean = true
    val sysElevation = App.INSTANCE.resources.getDimension(R.dimen.design_appbar_elevation).toInt()

    init {
        upConfig()
        isGooglePlay = App.INSTANCE.channel == "google"
        isCoolApk = App.INSTANCE.channel == "coolApk"
    }

    fun upConfig() {
        upEInkMode()
        upReplaceEnableDefault()
    }

    fun isNightTheme(context: Context): Boolean {
        return when (context.getPrefString(PreferKey.themeMode, "0")) {
            "1" -> false
            "2" -> true
            "3" -> false
            else -> context.sysIsDarkMode()
        }
    }

    var isNightTheme: Boolean
        get() = isNightTheme(App.INSTANCE)
        set(value) {
            if (isNightTheme != value) {
                if (value) {
                    App.INSTANCE.putPrefString(PreferKey.themeMode, "2")
                } else {
                    App.INSTANCE.putPrefString(PreferKey.themeMode, "1")
                }
            }
        }

    fun upEInkMode() {
        isEInkMode = App.INSTANCE.getPrefString(PreferKey.themeMode) == "3"
    }

    var isTransparentStatusBar: Boolean
        get() = App.INSTANCE.getPrefBoolean(PreferKey.transparentStatusBar)
        set(value) {
            App.INSTANCE.putPrefBoolean(PreferKey.transparentStatusBar, value)
        }

    val requestedDirection: String?
        get() = App.INSTANCE.getPrefString(R.string.pk_requested_direction)

    var backupPath: String?
        get() = App.INSTANCE.getPrefString(PreferKey.backupPath)
        set(value) {
            if (value.isNullOrEmpty()) {
                App.INSTANCE.removePref(PreferKey.backupPath)
            } else {
                App.INSTANCE.putPrefString(PreferKey.backupPath, value)
            }
        }

    var isShowRSS: Boolean
        get() = App.INSTANCE.getPrefBoolean(PreferKey.showRss, true)
        set(value) {
            App.INSTANCE.putPrefBoolean(PreferKey.showRss, value)
        }

    val autoRefreshBook: Boolean
        get() = App.INSTANCE.getPrefBoolean(R.string.pk_auto_refresh)

    var threadCount: Int
        get() = App.INSTANCE.getPrefInt(PreferKey.threadCount, 16)
        set(value) {
            App.INSTANCE.putPrefInt(PreferKey.threadCount, value)
        }

    var importBookPath: String?
        get() = App.INSTANCE.getPrefString("importBookPath")
        set(value) {
            if (value == null) {
                App.INSTANCE.removePref("importBookPath")
            } else {
                App.INSTANCE.putPrefString("importBookPath", value)
            }
        }

    var ttsSpeechRate: Int
        get() = App.INSTANCE.getPrefInt(PreferKey.ttsSpeechRate, 5)
        set(value) {
            App.INSTANCE.putPrefInt(PreferKey.ttsSpeechRate, value)
        }

    var chineseConverterType: Int
        get() = App.INSTANCE.getPrefInt(PreferKey.chineseConverterType)
        set(value) {
            App.INSTANCE.putPrefInt(PreferKey.chineseConverterType, value)
        }

    var systemTypefaces: Int
        get() = App.INSTANCE.getPrefInt(PreferKey.systemTypefaces)
        set(value) {
            App.INSTANCE.putPrefInt(PreferKey.systemTypefaces, value)
        }

    var elevation: Int
        @SuppressLint("PrivateResource")
        get() = App.INSTANCE.getPrefInt(PreferKey.barElevation, sysElevation)
        set(value) {
            App.INSTANCE.putPrefInt(PreferKey.barElevation, value)
        }

    val autoChangeSource: Boolean
        get() = App.INSTANCE.getPrefBoolean(PreferKey.autoChangeSource, true)

    val readBodyToLh: Boolean
        get() = App.INSTANCE.getPrefBoolean(PreferKey.readBodyToLh, true)

    fun upReplaceEnableDefault() {
        replaceEnableDefault =
            App.INSTANCE.getPrefBoolean(PreferKey.replaceEnableDefault, true)
    }

}

