package io.legado.app.constant

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.legado.app.App
import io.legado.app.R
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

object AppConst {
    const val channelIdDownload = "channel_download"
    const val channelIdReadAloud = "channel_read_aloud"
    const val channelIdWeb = "channel_web"

    const val APP_TAG = "Legado"
    const val RC_IMPORT_YUEDU_DATA = 100

    const val userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.2357.134 Safari/537.36"

    val SCRIPT_ENGINE: ScriptEngine = ScriptEngineManager().getEngineByName("rhino")

    val NOT_AVAILABLE = App.INSTANCE.getString(R.string.not_available)

    val GSON_CONVERTER: Gson = GsonBuilder()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .setDateFormat("yyyy-MM-dd HH:mm:ssZ")
        .create()
}