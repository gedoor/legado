package io.legado.app.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import org.jetbrains.anko.attempt

val GSON: Gson by lazy { GsonBuilder().create() }

inline fun <reified T> Gson.fromJson(json: String): T = fromJson(json, T::class.java)

inline fun <reified T> Gson.fromJsonArray(json: String): ArrayList<T>? {
    return attempt {
        with(JsonParser().parse(json).asJsonArray) {
            val result = ArrayList<T>()
            for (obj in this) {
                attempt { fromJson(obj, T::class.java) }.value?.run { result.add(this) }
            }
            result
        }
    }.value
}