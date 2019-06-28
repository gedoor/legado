package io.legado.app.constant

import io.legado.app.App
import io.legado.app.R
import javax.script.ScriptEngineManager

object AppConst {
    const val channelIdDownload = "channel_download"
    const val channelIdReadAloud = "channel_read_aloud"
    const val channelIdWeb = "channel_web"

    const val APP_TAG = "Legado"
    const val RC_IMPORT_YUEDU_DATA = 100

    val SCRIPT_ENGINE = ScriptEngineManager().getEngineByName("rhino")

    val NOT_AVAILABLE = App.INSTANCE.getString(R.string.not_available)

}