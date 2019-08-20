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

object ReadBookConfig {
    private val configList = arrayListOf<Config>()
    private var styleSelect
        get() = App.INSTANCE.getPrefInt("readStyleSelect")
        set(value) = App.INSTANCE.putPrefInt("readStyleSelect", value)
    var bg: Drawable? = null

    init {
        val configFile = File(App.INSTANCE.filesDir.absolutePath + File.separator + "config")
        val json = if (configFile.exists()) {
            String(configFile.readBytes())
        } else {
            String(App.INSTANCE.assets.open("readConfig.json").readBytes())
        }
        GSON.fromJsonArray<Config>(json)?.let {
            configList.clear()
            configList.addAll(it)
        } ?: reset()
    }

    fun getConfig(): Config {
        return configList[styleSelect]
    }

    fun upBg() {
        getConfig().apply {
            when (bgType) {
                0 -> bg = ColorDrawable(Color.parseColor(bgStr))

            }
        }
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
        val json = String(App.INSTANCE.assets.open("readConfig.json").readBytes())
        GSON.fromJsonArray<Config>(json)?.let {
            configList.clear()
            configList.addAll(it)
        }
    }

    data class Config(
        var bgStr: String = "#F3F3F3",
        var bgInt: Int = 0,
        var bgType: Int = 0,
        var darkStatusIcon: Boolean = true,
        var textColor: String = "#3E3D3B",
        var textSize: Int = 16,
        var letterSpacing: Int = 1,
        var lineSpacingExtra: Int = 15,
        var lineSpacingMultiplier: Int = 3
    )

}