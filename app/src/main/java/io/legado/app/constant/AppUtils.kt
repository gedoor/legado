package io.legado.app.constant

import com.google.gson.Gson
import com.google.gson.GsonBuilder

object AppUtils {
        val GSON_CONVERTER: Gson = GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .setDateFormat("yyyy-MM-dd HH:mm:ssZ")
            .create()
}