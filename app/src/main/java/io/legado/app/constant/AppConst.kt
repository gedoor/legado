package io.legado.app.constant

import android.annotation.SuppressLint
import android.provider.Settings
import io.legado.app.R
import splitties.init.appCtx
import java.text.SimpleDateFormat
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

@SuppressLint("SimpleDateFormat")
object AppConst {

    const val APP_TAG = "Legado"

    const val channelIdDownload = "channel_download"
    const val channelIdReadAloud = "channel_read_aloud"
    const val channelIdWeb = "channel_web"

    const val UA_NAME = "User-Agent"

    val SCRIPT_ENGINE: ScriptEngine by lazy {
        ScriptEngineManager().getEngineByName("rhino")
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

    val keyboardToolChars: List<String> by lazy {
        arrayListOf(
            "❓", "@css:", "<js></js>", "{{}}", "&&", "%%", "||", "//", "$.", "@",
            "\\", ":", "class", "id", "href", "textNodes", "ownText", "all", "html",
            "[", "]", "<", ">", "#", "!", ".", "+", "-", "*", "="
        )
    }

    const val bookGroupAllId = -1L
    const val bookGroupLocalId = -2L
    const val bookGroupAudioId = -3L
    const val bookGroupNoneId = -4L

    const val notificationIdRead = 1144771
    const val notificationIdAudio = 1144772
    const val notificationIdWeb = 1144773
    const val notificationIdDownload = 1144774

    val urlOption: String by lazy {
        """
        ,{
        "charset": "",
        "method": "POST",
        "body": "",
        "headers": {"User-Agent": ""}
        }
        """.trimIndent()
    }

    val menuViewNames = arrayOf(
        "com.android.internal.view.menu.ListMenuItemView",
        "androidx.appcompat.view.menu.ListMenuItemView"
    )

    val sysElevation = appCtx.resources.getDimension(R.dimen.design_appbar_elevation).toInt()

    val darkWebViewJs by lazy {
        """
            document.body.style.backgroundColor = "#222222";
            document.getElementsByTagName('body')[0].style.webkitTextFillColor = '#8a8a8a';
        """.trimIndent()
    }

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
}
