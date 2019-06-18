package io.legado.app.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import org.jetbrains.anko.attempt

val GSON: Gson = GsonBuilder().create()

inline fun <reified T> Gson.fromJson(json: String): T = fromJson(json, T::class.java)

inline fun <reified T> Gson.arrayFromJson(json: String): ArrayList<T>? = run {
    return@run attempt {
        val result = ArrayList<T>()
        val parser = JsonParser()
        val jArray = parser.parse(json).asJsonArray
        jArray?.let {
            for (obj in it) {
                attempt { fromJson(obj, T::class.java) }.value?.run { result.add(this) }
            }
        }
        result
    }.value
}