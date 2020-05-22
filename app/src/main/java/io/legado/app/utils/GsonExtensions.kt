package io.legado.app.utils

import com.google.gson.*
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import org.jetbrains.anko.attempt
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.math.ceil


val GSON: Gson by lazy {
    GsonBuilder()
        .registerTypeAdapter(
            object : TypeToken<Map<String?, Any?>?>() {}.type,
            MapDeserializerDoubleAsIntFix()
        )
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

    fun read(`in`: JsonElement): Any? {
        when {
            `in`.isJsonArray -> {
                val list: MutableList<Any?> = ArrayList()
                val arr = `in`.asJsonArray
                for (anArr in arr) {
                    list.add(read(anArr))
                }
                return list
            }
            `in`.isJsonObject -> {
                val map: MutableMap<String, Any?> =
                    LinkedTreeMap()
                val obj = `in`.asJsonObject
                val entitySet =
                    obj.entrySet()
                for ((key, value) in entitySet) {
                    map[key] = read(value)
                }
                return map
            }
            `in`.isJsonPrimitive -> {
                val prim = `in`.asJsonPrimitive
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