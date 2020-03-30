package io.legado.app.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import org.jetbrains.anko.attempt
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

val GSON: Gson by lazy {
    GsonBuilder()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create()
}

inline fun <reified T> genericType(): Type = object : TypeToken<T>() {}.type

@Throws(JsonSyntaxException::class)
inline fun <reified T> Gson.fromJsonObject(json: String?): T? {//可转成任意类型
    return attempt {
        val result: T? = fromJson(json, genericType<T>())
        result
    }.value
}

@Throws(JsonSyntaxException::class)
inline fun <reified T> Gson.fromJsonArray(json: String?): List<T>? {
    return attempt {
        val result: List<T>? = fromJson(json, ParameterizedTypeImpl(T::class.java))
        result
    }.value
}

class ParameterizedTypeImpl(private val clazz: Class<*>) : ParameterizedType {
    override fun getRawType(): Type = List::class.java

    override fun getOwnerType(): Type? = null

    override fun getActualTypeArguments(): Array<Type> = arrayOf(clazz)
}
