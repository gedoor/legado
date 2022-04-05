package io.legado.app.utils

import com.google.gson.*
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonWriter
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.math.ceil


val GSON: Gson by lazy {
    GsonBuilder()
        .registerTypeAdapter(
            object : TypeToken<Map<String?, Any?>?>() {}.type,
            MapDeserializerDoubleAsIntFix()
        )
        .registerTypeAdapter(Int::class.java, IntJsonDeserializer())
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create()
}

inline fun <reified T> genericType(): Type = object : TypeToken<T>() {}.type

inline fun <reified T> Gson.fromJsonObject(json: String?): Result<T?> {
    return kotlin.runCatching {
        fromJson(json, genericType<T>()) as? T
    }
}

inline fun <reified T> Gson.fromJsonArray(json: String?): Result<List<T>?> {
    return kotlin.runCatching {
        fromJson(json, ParameterizedTypeImpl(T::class.java)) as? List<T>
    }
}

inline fun <reified T> Gson.fromJsonObject(inputStream: InputStream?): Result<T?> {
    return kotlin.runCatching {
        val reader = InputStreamReader(inputStream)
        fromJson(reader, genericType<T>()) as? T
    }
}

inline fun <reified T> Gson.fromJsonArray(inputStream: InputStream?): Result<List<T>?> {
    return kotlin.runCatching {
        val reader = InputStreamReader(inputStream)
        fromJson(reader, ParameterizedTypeImpl(T::class.java)) as? List<T>
    }
}

fun Gson.writeToOutputStream(out: OutputStream, any: Any) {
    val writer = JsonWriter(OutputStreamWriter(out, "UTF-8"))
    writer.setIndent("  ")
    if (any is List<*>) {
        writer.beginArray()
        any.forEach {
            it?.let {
                toJson(it, it::class.java, writer)
            }
        }
        writer.endArray()
    } else {
        toJson(any, any::class.java, writer)
    }
    writer.close()
}

class ParameterizedTypeImpl(private val clazz: Class<*>) : ParameterizedType {
    override fun getRawType(): Type = List::class.java

    override fun getOwnerType(): Type? = null

    override fun getActualTypeArguments(): Array<Type> = arrayOf(clazz)
}

/**
 * int类型转化失败时跳过
 */
class IntJsonDeserializer : JsonDeserializer<Int?> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Int? {
        return when {
            json.isJsonPrimitive -> {
                val prim = json.asJsonPrimitive
                if (prim.isNumber) {
                    prim.asNumber.toInt()
                } else {
                    null
                }
            }
            else -> null
        }
    }

}


/**
 * 修复Int变为Double的问题
 */
class MapDeserializerDoubleAsIntFix :
    JsonDeserializer<Map<String, Any?>?> {

    @Throws(JsonParseException::class)
    override fun deserialize(
        jsonElement: JsonElement,
        type: Type,
        jsonDeserializationContext: JsonDeserializationContext
    ): Map<String, Any?>? {
        @Suppress("unchecked_cast")
        return read(jsonElement) as? Map<String, Any?>
    }

    fun read(json: JsonElement): Any? {
        when {
            json.isJsonArray -> {
                val list: MutableList<Any?> = ArrayList()
                val arr = json.asJsonArray
                for (anArr in arr) {
                    list.add(read(anArr))
                }
                return list
            }
            json.isJsonObject -> {
                val map: MutableMap<String, Any?> =
                    LinkedTreeMap()
                val obj = json.asJsonObject
                val entitySet =
                    obj.entrySet()
                for ((key, value) in entitySet) {
                    map[key] = read(value)
                }
                return map
            }
            json.isJsonPrimitive -> {
                val prim = json.asJsonPrimitive
                when {
                    prim.isBoolean -> {
                        return prim.asBoolean
                    }
                    prim.isString -> {
                        return prim.asString
                    }
                    prim.isNumber -> {
                        val num: Number = prim.asNumber
                        // here you can handle double int/long values
                        // and return any type you want
                        // this solution will transform 3.0 float to long values
                        return if (ceil(num.toDouble()) == num.toLong().toDouble()) {
                            num.toLong()
                        } else {
                            num.toDouble()
                        }
                    }
                }
            }
        }
        return null
    }

}