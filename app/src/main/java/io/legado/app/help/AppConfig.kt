package io.legado.app.help

import android.content.Context
import android.content.SharedPreferences
import io.legado.app.constant.AppConst
import io.legado.app.constant.PreferKey
import io.legado.app.utils.*
import splitties.init.appCtx

@Suppress("MemberVisibilityCanBePrivate")
object AppConfig : SharedPreferences.OnSharedPreferenceChangeListener {
    val isGooglePlay = appCtx.channel == "google"
    var userAgent: String = getPrefUserAgent()
    var isEInkMode = appCtx.getPrefString(PreferKey.themeMode) == "3"
    var clickActionTL = appCtx.getPrefInt(PreferKey.clickActionTL, 2)
    var clickActionTC = appCtx.getPrefInt(PreferKey.clickActionTC, 2)
    var clickActionTR = appCtx.getPrefInt(PreferKey.clickActionTR, 1)
    var clickActionML = appCtx.getPrefInt(PreferKey.clickActionML, 2)
    var clickActionMC = appCtx.getPrefInt(PreferKey.clickActionMC, 0)
    var clickActionMR = appCtx.getPrefInt(PreferKey.clickActionMR, 1)
    var clickActionBL = appCtx.getPrefInt(PreferKey.clickActionBL, 2)
    var clickActionBC = appCtx.getPrefInt(PreferKey.clickActionBC, 1)
    var clickActionBR = appCtx.getPrefInt(PreferKey.clickActionBR, 1)

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PreferKey.themeMode -> isEInkMode = appCtx.getPrefString(PreferKey.themeMode) == "3"
            PreferKey.clickActionTL -> clickActionTL =
                appCtx.getPrefInt(PreferKey.clickActionTL, 2)
            PreferKey.clickActionTC -> clickActionTC =
                appCtx.getPrefInt(PreferKey.clickActionTC, 2)
            PreferKey.clickActionTR -> clickActionTR =
                appCtx.getPrefInt(PreferKey.clickActionTR, 2)
            PreferKey.clickActionML -> clickActionML =
                appCtx.getPrefInt(PreferKey.clickActionML, 2)
            PreferKey.clickActionMC -> clickActionMC =
                appCtx.getPrefInt(PreferKey.clickActionMC, 2)
            PreferKey.clickActionMR -> clickActionMR =
                appCtx.getPrefInt(PreferKey.clickActionMR, 2)
            PreferKey.clickActionBL -> clickActionBL =
                appCtx.getPrefInt(PreferKey.clickActionBL, 2)
            PreferKey.clickActionBC -> clickActionBC =
                appCtx.getPrefInt(PreferKey.clickActionBC, 2)
            PreferKey.clickActionBR -> clickActionBR =
                appCtx.getPrefInt(PreferKey.clickActionBR, 2)
            PreferKey.readBodyToLh -> ReadBookConfig.readBodyToLh =
                appCtx.getPrefBoolean(PreferKey.readBodyToLh, true)
            PreferKey.useZhLayout -> ReadBookConfig.useZhLayout =
                appCtx.getPrefBoolean(PreferKey.useZhLayout)
            PreferKey.userAgent -> userAgent = getPrefUserAgent()
        }
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
        get() = isNightTheme(appCtx)
        set(value) {
            if (isNightTheme != value) {
                if (value) {
                    appCtx.putPrefString(PreferKey.themeMode, "2")
                } else {
                    appCtx.putPrefString(PreferKey.themeMode, "1")
                }
            }
        }

    val isTransparentStatusBar: Boolean
        get() = appCtx.getPrefBoolean(PreferKey.transparentStatusBar, true)

    val immNavigationBar: Boolean
        get() = appCtx.getPrefBoolean(PreferKey.immNavigationBar, true)

    val screenOrientation: String?
        get() = appCtx.getPrefString(PreferKey.screenOrientation)

    var backupPath: String?
        get() = appCtx.getPrefString(PreferKey.backupPath)
        set(value) {
            if (value.isNullOrEmpty()) {
                appCtx.removePref(PreferKey.backupPath)
            } else {
                appCtx.putPrefString(PreferKey.backupPath, value)
            }
        }

    val isShowRSS: Boolean
        get() = appCtx.getPrefBoolean(PreferKey.showRss, true)

    val autoRefreshBook: Boolean
        get() = appCtx.getPrefBoolean(PreferKey.autoRefresh)

    var threadCount: Int
        get() = appCtx.getPrefInt(PreferKey.threadCount, 16)
        set(value) {
            appCtx.putPrefInt(PreferKey.threadCount, value)
        }

    var importBookPath: String?
        get() = appCtx.getPrefString("importBookPath")
        set(value) {
            if (value == null) {
                appCtx.removePref("importBookPath")
            } else {
                appCtx.putPrefString("importBookPath", value)
            }
        }

    var ttsSpeechRate: Int
        get() = appCtx.getPrefInt(PreferKey.ttsSpeechRate, 5)
        set(value) {
            appCtx.putPrefInt(PreferKey.ttsSpeechRate, value)
        }

    var chineseConverterType: Int
        get() = appCtx.getPrefInt(PreferKey.chineseConverterType)
        set(value) {
            appCtx.putPrefInt(PreferKey.chineseConverterType, value)
        }

    var systemTypefaces: Int
        get() = appCtx.getPrefInt(PreferKey.systemTypefaces)
        set(value) {
            appCtx.putPrefInt(PreferKey.systemTypefaces, value)
        }

    var elevation: Int
        get() = appCtx.getPrefInt(PreferKey.barElevation, AppConst.sysElevation)
        set(value) {
            appCtx.putPrefInt(PreferKey.barElevation, value)
        }

    var exportCharset: String
        get() {
            val c = appCtx.getPrefString(PreferKey.exportCharset)
            if (c.isNullOrBlank()) {
                return "UTF-8"
            }
            return c
        }
        set(value) {
            appCtx.putPrefString(PreferKey.exportCharset, value)
        }

    var exportUseReplace: Boolean
        get() = appCtx.getPrefBoolean(PreferKey.exportUseReplace, true)
        set(value) {
            appCtx.putPrefBoolean(PreferKey.exportUseReplace, value)
        }

    var exportToWebDav: Boolean
        get() = appCtx.getPrefBoolean(PreferKey.exportToWebDav)
        set(value) {
            appCtx.putPrefBoolean(PreferKey.exportToWebDav, value)
        }

    val autoChangeSource: Boolean
        get() = appCtx.getPrefBoolean(PreferKey.autoChangeSource, true)

    val changeSourceLoadInfo get() = appCtx.getPrefBoolean(PreferKey.changeSourceLoadToc)

    val changeSourceLoadToc get() = appCtx.getPrefBoolean(PreferKey.changeSourceLoadToc)

    val importKeepName get() = appCtx.getPrefBoolean(PreferKey.importKeepName)

    val syncBookProgress get() = appCtx.getPrefBoolean(PreferKey.syncBookProgress, true)

    val preDownload get() = appCtx.getPrefBoolean(PreferKey.preDownload, true)

    val mediaButtonOnExit get() = appCtx.getPrefBoolean("mediaButtonOnExit", true)

    val replaceEnableDefault get() =  appCtx.getPrefBoolean(PreferKey.replaceEnableDefault, true)

    private fun getPrefUserAgent(): String {
        val ua = appCtx.getPrefString(PreferKey.userAgent)
        if (ua.isNullOrBlank()) {
            return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Safari/537.36"
        }
        return ua
    }
}

