package io.legado.app.help

import android.content.Context
import android.content.SharedPreferences
import io.legado.app.App
import io.legado.app.R
import io.legado.app.constant.AppConst
import io.legado.app.constant.PreferKey
import io.legado.app.utils.*

@Suppress("MemberVisibilityCanBePrivate")
object AppConfig : SharedPreferences.OnSharedPreferenceChangeListener {
    private val context get() = App.INSTANCE
    val isGooglePlay = context.channel == "google"
    var userAgent: String = getPrefUserAgent()
    var replaceEnableDefault = context.getPrefBoolean(PreferKey.replaceEnableDefault, true)
    var isEInkMode = context.getPrefString(PreferKey.themeMode) == "3"
    var clickActionTL = context.getPrefInt(PreferKey.clickActionTL, 2)
    var clickActionTC = context.getPrefInt(PreferKey.clickActionTC, 2)
    var clickActionTR = context.getPrefInt(PreferKey.clickActionTR, 1)
    var clickActionML = context.getPrefInt(PreferKey.clickActionML, 2)
    var clickActionMC = context.getPrefInt(PreferKey.clickActionMC, 0)
    var clickActionMR = context.getPrefInt(PreferKey.clickActionMR, 1)
    var clickActionBL = context.getPrefInt(PreferKey.clickActionBL, 2)
    var clickActionBC = context.getPrefInt(PreferKey.clickActionBC, 1)
    var clickActionBR = context.getPrefInt(PreferKey.clickActionBR, 1)

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PreferKey.themeMode -> isEInkMode = context.getPrefString(PreferKey.themeMode) == "3"
            PreferKey.clickActionTL -> clickActionTL =
                context.getPrefInt(PreferKey.clickActionTL, 2)
            PreferKey.clickActionTC -> clickActionTC =
                context.getPrefInt(PreferKey.clickActionTC, 2)
            PreferKey.clickActionTR -> clickActionTR =
                context.getPrefInt(PreferKey.clickActionTR, 2)
            PreferKey.clickActionML -> clickActionML =
                context.getPrefInt(PreferKey.clickActionML, 2)
            PreferKey.clickActionMC -> clickActionMC =
                context.getPrefInt(PreferKey.clickActionMC, 2)
            PreferKey.clickActionMR -> clickActionMR =
                context.getPrefInt(PreferKey.clickActionMR, 2)
            PreferKey.clickActionBL -> clickActionBL =
                context.getPrefInt(PreferKey.clickActionBL, 2)
            PreferKey.clickActionBC -> clickActionBC =
                context.getPrefInt(PreferKey.clickActionBC, 2)
            PreferKey.clickActionBR -> clickActionBR =
                context.getPrefInt(PreferKey.clickActionBR, 2)
            PreferKey.readBodyToLh -> ReadBookConfig.readBodyToLh =
                context.getPrefBoolean(PreferKey.readBodyToLh, true)
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
        get() = isNightTheme(context)
        set(value) {
            if (isNightTheme != value) {
                if (value) {
                    context.putPrefString(PreferKey.themeMode, "2")
                } else {
                    context.putPrefString(PreferKey.themeMode, "1")
                }
            }
        }

    val isTransparentStatusBar: Boolean
        get() = context.getPrefBoolean(PreferKey.transparentStatusBar, true)

    val immNavigationBar: Boolean
        get() = context.getPrefBoolean(PreferKey.immNavigationBar, true)

    val screenOrientation: String?
        get() = context.getPrefString(PreferKey.screenOrientation)

    var backupPath: String?
        get() = context.getPrefString(PreferKey.backupPath)
        set(value) {
            if (value.isNullOrEmpty()) {
                context.removePref(PreferKey.backupPath)
            } else {
                context.putPrefString(PreferKey.backupPath, value)
            }
        }

    val isShowRSS: Boolean
        get() = context.getPrefBoolean(PreferKey.showRss, true)

    val autoRefreshBook: Boolean
        get() = context.getPrefBoolean(R.string.pk_auto_refresh)

    var threadCount: Int
        get() = context.getPrefInt(PreferKey.threadCount, 16)
        set(value) {
            context.putPrefInt(PreferKey.threadCount, value)
        }

    var importBookPath: String?
        get() = context.getPrefString("importBookPath")
        set(value) {
            if (value == null) {
                context.removePref("importBookPath")
            } else {
                context.putPrefString("importBookPath", value)
            }
        }

    var ttsSpeechRate: Int
        get() = context.getPrefInt(PreferKey.ttsSpeechRate, 5)
        set(value) {
            context.putPrefInt(PreferKey.ttsSpeechRate, value)
        }

    var chineseConverterType: Int
        get() = context.getPrefInt(PreferKey.chineseConverterType)
        set(value) {
            context.putPrefInt(PreferKey.chineseConverterType, value)
        }

    var systemTypefaces: Int
        get() = context.getPrefInt(PreferKey.systemTypefaces)
        set(value) {
            context.putPrefInt(PreferKey.systemTypefaces, value)
        }

    var elevation: Int
        get() = context.getPrefInt(PreferKey.barElevation, AppConst.sysElevation)
        set(value) {
            context.putPrefInt(PreferKey.barElevation, value)
        }

    val autoChangeSource: Boolean
        get() = context.getPrefBoolean(PreferKey.autoChangeSource, true)

    val changeSourceLoadInfo get() = context.getPrefBoolean(PreferKey.changeSourceLoadToc)

    val changeSourceLoadToc get() = context.getPrefBoolean(PreferKey.changeSourceLoadToc)

    val importKeepName get() = context.getPrefBoolean(PreferKey.importKeepName)

    val syncBookProgress get() = context.getPrefBoolean(PreferKey.syncBookProgress, true)

    val preDownload get() = context.getPrefBoolean(PreferKey.preDownload, true)

    private fun getPrefUserAgent(): String {
        val ua = context.getPrefString(PreferKey.userAgent)
        if (ua.isNullOrBlank()) {
            return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Safari/537.36"
        }
        return ua
    }
}

