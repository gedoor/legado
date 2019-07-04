package io.legado.app.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import org.jetbrains.anko.attempt

val GSON: Gson by lazy { GsonBuilder().create() }

inline fun <reified T> genericType() = object : TypeToken<T>() {}.type

inline fun <reified T> Gson.fromJsonObject(json: String?): T? {//可转成任意类型
    return attempt {
        val result: T? = fromJson(json, genericType<T>())
        result
    }.value
}

inline fun <reified T> Gson.fromJsonArray(json: String?): List<T>? {
    return attempt {
        val result: List<T>? = fromJson(json, genericType<List<T>>())
        result
    }.value
}