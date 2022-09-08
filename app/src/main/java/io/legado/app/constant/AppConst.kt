package io.legado.app.constant

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.provider.Settings
import com.script.javascript.RhinoScriptEngine
import io.legado.app.BuildConfig
import splitties.init.appCtx
import java.text.SimpleDateFormat

@SuppressLint("SimpleDateFormat")
object AppConst {

    const val APP_TAG = "Legado"

    const val channelIdDownload = "channel_download"
    const val channelIdReadAloud = "channel_read_aloud"
    const val channelIdWeb = "channel_web"

    const val UA_NAME = "User-Agent"

    const val MAX_THREAD = 9

    val SCRIPT_ENGINE: RhinoScriptEngine by lazy {
        RhinoScriptEngine()
    }

    val timeFormat: SimpleDateFormat by lazy {
        SimpleDateFormat("HH:mm")
    }

    val dateFormat: SimpleDateFormat by lazy {
        SimpleDateFormat("yyyy/MM/dd HH:mm")
    }

    val fileNameFormat: SimpleDateFormat by lazy {
        SimpleDateFormat("yy-MM-dd-HH-mm-ss")
    }

    const val rootGroupId = -100L
    const val bookGroupAllId = -1L
    const val bookGroupLocalId = -2L
    const val bookGroupAudioId = -3L
    const val bookGroupNoneId = -4L

    const val notificationIdRead = -1122391
    const val notificationIdAudio = -1122392
    const val notificationIdCache = -1122393
    const val notificationIdWeb = -1122394
    const val notificationIdDownload = -1122395
    const val notificationIdCheckSource = -1122395

    const val imagePathKey = "imagePath"

    val menuViewNames = arrayOf(
        "com.android.internal.view.menu.ListMenuItemView",
        "androidx.appcompat.view.menu.ListMenuItemView"
    )

    @SuppressLint("PrivateResource")
    val sysElevation = appCtx.resources
        .getDimension(com.google.android.material.R.dimen.design_appbar_elevation)
        .toInt()

    val androidId: String by lazy {
        Settings.System.getString(appCtx.contentResolver, Settings.Secure.ANDROID_ID) ?: "null"
    }

    val appInfo: AppInfo by lazy {
        val appInfo = AppInfo()
        @Suppress("DEPRECATION")
        appCtx.packageManager.getPackageInfo(appCtx.packageName, PackageManager.GET_ACTIVITIES)
            ?.let {
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

    /**
     * The authority of a FileProvider defined in a <provider> element in your app's manifest.
     */
    const val authority = BuildConfig.APPLICATION_ID + ".fileProvider"

}
