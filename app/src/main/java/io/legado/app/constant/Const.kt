package io.legado.app.constant

import android.provider.Settings
import splitties.init.appCtx

val androidId: String by lazy {
    Settings.System.getString(appCtx.contentResolver, Settings.Secure.ANDROID_ID)
}

val appInfo: AppInfo by lazy {
    val appInfo = AppInfo()
    appCtx.packageManager.getPackageInfo(appCtx.packageName, 0)?.let {
        appInfo.versionName = it.versionName
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            appInfo.versionCode = it.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            appInfo.versionCode = it.versionCode.toLong()
        }
    }
    appInfo
}

val charsets =
    arrayListOf("UTF-8", "GB2312", "GB18030", "GBK", "Unicode", "UTF-16", "UTF-16LE", "ASCII")

data class AppInfo(
    var versionCode: Long = 0L,
    var versionName: String = ""
)