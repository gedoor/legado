package io.legado.app.constant

import android.annotation.SuppressLint
import io.legado.app.App
import io.legado.app.R
import java.text.SimpleDateFormat
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

@SuppressLint("SimpleDateFormat")
object AppConst {
    const val channelIdDownload = "channel_download"
    const val channelIdReadAloud = "channel_read_aloud"
    const val channelIdWeb = "channel_web"

    const val APP_TAG = "Legado"
    const val RC_IMPORT_YUEDU_DATA = 100

    const val userAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.142 Safari/537.36"

    val SCRIPT_ENGINE: ScriptEngine = ScriptEngineManager().getEngineByName("rhino")

    val NOT_AVAILABLE = App.INSTANCE.getString(R.string.not_available)

    val TIME_FORMAT: SimpleDateFormat by lazy {
        SimpleDateFormat("HH:mm")
    }

    val keyboardToolChars = arrayListOf(
        "@",
        "&",
        "|",
        "%",
        "/",
        ":",
        "[",
        "]",
        "{",
        "}",
        "<",
        ">",
        "\\",
        "$",
        "#",
        "!",
        ".",
        "href",
        "src",
        "textNodes",
        "xpath",
        "json",
        "css",
        "id",
        "class",
        "tag"
    )
}