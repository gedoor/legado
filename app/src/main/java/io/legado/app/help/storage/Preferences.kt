package io.legado.app.help.storage

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import java.io.File

object Preferences {

    /**
     * 用反射生成 SharedPreferences
     * @param context
     * @param dir
     * @param fileName 文件名,不需要 '.xml' 后缀
     * @return
     */
    fun getSharedPreferences(
        context: Context,
        dir: String,
        fileName: String
    ): SharedPreferences? {
        try {
            // 获取 ContextWrapper对象中的mBase变量。该变量保存了 ContextImpl 对象
            val fieldMBase = ContextWrapper::class.java.getDeclaredField("mBase")
            fieldMBase.isAccessible = true
            // 获取 mBase变量
            val objMBase = fieldMBase.get(context)
            // 获取 ContextImpl。mPreferencesDir变量，该变量保存了数据文件的保存路径
            val fieldMPreferencesDir = objMBase.javaClass.getDeclaredField("mPreferencesDir")
            fieldMPreferencesDir.isAccessible = true
            // 创建自定义路径
            val file = File(dir)
            // 修改mPreferencesDir变量的值
            fieldMPreferencesDir.set(objMBase, file)
            // 返回修改路径以后的 SharedPreferences :%FILE_PATH%/%fileName%.xml
            return context.getSharedPreferences(fileName, Activity.MODE_PRIVATE)
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
        return null
    }
}