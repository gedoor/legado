@file:Suppress("unused")

package io.legado.app.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.core.content.edit
import java.io.File

/**
 * 获取自定义路径的SharedPreferences, 用反射生成 SharedPreferences
 * @param dir 目录路径
 * @param fileName 文件名,不需要 '.xml' 后缀
 * @return SharedPreferences
 */
@SuppressLint("DiscouragedPrivateApi")
fun Context.getSharedPreferences(
    dir: String,
    fileName: String
): SharedPreferences? {
    try {
        // 获取 ContextWrapper对象中的mBase变量。该变量保存了 ContextImpl 对象
        val fieldMBase = ContextWrapper::class.java.getDeclaredField("mBase")
        fieldMBase.isAccessible = true
        // 获取 mBase变量
        val objMBase = fieldMBase.get(this)
        // 获取 ContextImpl。mPreferencesDir变量，该变量保存了数据文件的保存路径
        val fieldMPreferencesDir = objMBase.javaClass.getDeclaredField("mPreferencesDir")
        fieldMPreferencesDir.isAccessible = true
        // 创建自定义路径
        val file = File(dir)
        // 修改mPreferencesDir变量的值
        fieldMPreferencesDir.set(objMBase, file)
        // 返回修改路径以后的 SharedPreferences :%FILE_PATH%/%fileName%.xml
        return getSharedPreferences(fileName, Activity.MODE_PRIVATE)
    } catch (e: NoSuchFieldException) {
        e.printOnDebug()
    } catch (e: IllegalArgumentException) {
        e.printOnDebug()
    } catch (e: IllegalAccessException) {
        e.printOnDebug()
    }
    return null
}

fun SharedPreferences.getString(key: String): String? {
    return getString(key, null)
}

fun SharedPreferences.putString(key: String, value: String) {
    edit {
        putString(key, value)
    }
}

fun SharedPreferences.getBoolean(key: String): Boolean {
    return getBoolean(key, false)
}

fun SharedPreferences.putBoolean(key: String, value: Boolean) {
    edit {
        putBoolean(key, value)
    }
}

fun SharedPreferences.getInt(key: String): Int {
    return getInt(key, 0)
}

fun SharedPreferences.putInt(key: String, value: Int) {
    edit {
        putInt(key, value)
    }
}

fun SharedPreferences.getLong(key: String): Long {
    return getLong(key, 0)
}

fun SharedPreferences.putLong(key: String, value: Long) {
    edit {
        putLong(key, value)
    }
}

fun SharedPreferences.getFloat(key: String): Float {
    return getFloat(key, 0f)
}

fun SharedPreferences.putFloat(key: String, value: Float) {
    edit {
        putFloat(key, value)
    }
}

fun SharedPreferences.remove(key: String) {
    edit {
        remove(key)
    }
}