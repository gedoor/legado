package io.legado.app.constant

import android.annotation.SuppressLint
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

    val userAgent: String by lazy {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Safari/537.36"
    }

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
            "※", "@", "&", "|", "%", "/", ":", "[", "]", "{", "}", "<", ">", "\\",
            "$", "#", "!", ".", "href", "src", "textNodes", "xpath", "json", "css",
            "id", "class", "tag"
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
}