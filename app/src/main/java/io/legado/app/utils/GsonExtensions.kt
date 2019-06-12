package io.legado.app.utils

import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

inline fun <reified T> Gson.fromJson(json: String): T = fromJson(json, T::class.java)

inline fun <reified T> Gson.arrayFromJson(json: String): ArrayList<T>? = kotlin.run {
    var result: ArrayList<T>? = null
    if (!TextUtils.isEmpty(json)) {
        val gson = GsonBuilder().create()
        try {
            val parser = JsonParser()
            val jArray = parser.parse(json).asJsonArray
            jArray?.let {
                result = java.util.ArrayList()
                for (obj in it) {
                    try {
                        val cse = gson.fromJson(obj, T::class.java)
                        result?.add(cse)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
    return result
}