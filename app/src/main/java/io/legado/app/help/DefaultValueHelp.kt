package io.legado.app.help

import io.legado.app.App
import io.legado.app.data.entities.HttpTTS
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonArray

object DefaultValueHelp {


    fun initHttpTTS() {
        val json = String(App.INSTANCE.assets.open("httpTTS.json").readBytes())
        GSON.fromJsonArray<HttpTTS>(json)?.let {
            App.db.httpTTSDao().insert(*it.toTypedArray())
        }
    }


}