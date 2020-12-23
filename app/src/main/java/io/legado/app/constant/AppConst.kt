package io.legado.app.constant

import android.annotation.SuppressLint
import io.legado.app.App
import io.legado.app.R
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

    val sysElevation = App.INSTANCE.resources.getDimension(R.dimen.design_appbar_elevation).toInt()
}
