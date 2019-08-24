package io.legado.app.help

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import io.legado.app.App
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonArray
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.putPrefInt
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

/**
 * 阅读界面配置
 */
object ReadBookConfig {
    private val configList: ArrayList<Config> by lazy {
        val list: ArrayList<Config> = arrayListOf()
        val configFile = File(App.INSTANCE.filesDir.absolutePath + File.separator + "config")
        val json = if (configFile.exists()) {
            String(configFile.readBytes())
        } else {
            String(App.INSTANCE.assets.open("readConfig.json").readBytes())
        }
        try {
            GSON.fromJsonArray<Config>(json)?.let {
                list.addAll(it)
            }
        } catch (e: Exception) {
            list.addAll(getOnError())
        }
        list
    }

    var styleSelect
        get() = App.INSTANCE.getPrefInt("readStyleSelect")
        set(value) = App.INSTANCE.putPrefInt("readStyleSelect", value)
    var bg: Drawable? = null

    fun getConfig(index: Int = styleSelect): Config {
        return configList[index]
    }

    fun upBg() {
        bg = getConfig().bgDrawable()
    }

    fun save() {
        val json = GSON.toJson(configList)
        val configFile = File(App.INSTANCE.filesDir.absolutePath + File.separator + "config")
        //获取流并存储
        try {
            BufferedWriter(FileWriter(configFile)).use { writer ->
                writer.write(json)
                writer.flush()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun reset() {
        try {
            val json = String(App.INSTANCE.assets.open("readConfig.json").readBytes())
            GSON.fromJsonArray<Config>(json)?.let {
                configList.clear()
                configList.addAll(it)
            }
        } catch (e: Exception) {
            configList.clear()
            configList.addAll(getOnError())
        }
    }

    private fun getOnError(): ArrayList<Config> {
        val list = arrayListOf<Config>()
        list.add(Config())
        list.add(Config())
        list.add(Config())
        list.add(Config())
        list.add(Config())
        return list
    }

    data class Config(
        var bgStr: String = "#015A86",
        var bgType: Int = 0,
        var darkStatusIcon: Boolean = true,
        var letterSpacing: Float = 1f,
        var lineSpacingExtra: Float = 15f,
        var lineSpacingMultiplier: Float = 3f,
        var paddingBottom: Int = 0,
        var paddingLeft: Int = 16,
        var paddingRight: Int = 16,
        var paddingTop: Int = 0,
        var textBold: Boolean = false,
        var textColor: String = "#3E3D3B",
        var textSize: Int = 15
    ) {
        fun textColor(): Int {
            return Color.parseColor(textColor)
        }

        fun bgDrawable(): Drawable {
            var bgDrawable: Drawable? = null
            when (bgType) {
                0 -> bgDrawable = ColorDrawable(Color.parseColor(bgStr))
                1 -> bgDrawable =
                    Drawable.createFromStream(App.INSTANCE.assets.open("bg" + File.separator + bgStr), "bg")
                else -> runCatching {
                    bgDrawable = Drawable.createFromPath(bgStr)
                }
            }
            return bgDrawable ?: ColorDrawable(Color.parseColor("#015A86"))
        }
    }
}