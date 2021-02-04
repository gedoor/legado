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

data class AppInfo(
    var versionCode: Long = 0L,
    var versionName: String = ""
)