package io.legado.app.help

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import io.legado.app.App
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.internal.toHexString
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

/**
 * 阅读界面配置
 */
object ReadBookConfig {
    private const val fileName = "readConfig.json"
    private val configList: ArrayList<Config> = arrayListOf<Config>()
        .apply {
            upConfig(this)
        }

    var styleSelect
        get() = App.INSTANCE.getPrefInt("readStyleSelect")
        set(value) = App.INSTANCE.putPrefInt("readStyleSelect", value)
    var bg: Drawable? = null

    fun getConfig(index: Int = styleSelect): Config {
        return configList[index]
    }

    fun upConfig(list: ArrayList<Config> = configList) {
        val configFile = File(App.INSTANCE.filesDir.absolutePath + File.separator + fileName)
        val json = if (configFile.exists()) {
            String(configFile.readBytes())
        } else {
            String(App.INSTANCE.assets.open(fileName).readBytes())
        }
        try {
            GSON.fromJsonArray<Config>(json)?.let {
                list.clear()
                list.addAll(it)
            }
        } catch (e: Exception) {
            list.clear()
            list.addAll(getOnError())
        }
    }

    fun upBg() {
        bg = getConfig().bgDrawable()
    }

    fun save() {
        GlobalScope.launch(IO) {
            val json = GSON.toJson(configList)
            val configFile = File(App.INSTANCE.filesDir.absolutePath + File.separator + fileName)
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
    }

    fun reset() {
        try {
            val json = String(App.INSTANCE.assets.open(fileName).readBytes())
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
        var bgStrNight: String = "#000000",
        var bgType: Int = 0,
        var bgTypeNight: Int = 0,
        var darkStatusIcon: Boolean = true,
        var letterSpacing: Float = 1f,
        var lineSpacingExtra: Int = 12,
        var lineSpacingMultiplier: Float = 1.2f,
        var paddingBottom: Int = 0,
        var paddingLeft: Int = 16,
        var paddingRight: Int = 16,
        var paddingTop: Int = 0,
        var textBold: Boolean = false,
        var textColor: String = "#3E3D3B",
        var textColorNight: String = "#adadad",
        var textSize: Int = 15
    ) {
        fun setBg(bgType: Int, bg: String) {
            if (App.INSTANCE.isNightTheme) {
                bgTypeNight = bgType
                bgStrNight = bg
            } else {
                this.bgType = bgType
                bgStr = bg
            }
        }

        fun setTextColor(color: Int) {
            if (App.INSTANCE.isNightTheme) {
                textColorNight = "#${color.toHexString()}"
            } else {
                textColor = "#${color.toHexString()}"
            }
        }

        fun textColor(): Int {
            return if (App.INSTANCE.isNightTheme) Color.parseColor(textColorNight)
            else Color.parseColor(textColor)
        }

        fun bgStr(): String {
            return if (App.INSTANCE.isNightTheme) bgStrNight
            else bgStr
        }

        fun bgType(): Int {
            return if (App.INSTANCE.isNightTheme) bgTypeNight
            else bgType
        }

        fun bgDrawable(): Drawable {
            var bgDrawable: Drawable? = null
            when (bgType()) {
                0 -> bgDrawable = ColorDrawable(Color.parseColor(bgStr()))
                1 -> bgDrawable =
                    Drawable.createFromStream(
                        App.INSTANCE.assets.open("bg" + File.separator + bgStr),
                        "bg"
                    )
                else -> runCatching {
                    bgDrawable = Drawable.createFromPath(bgStr)
                }
            }
            return bgDrawable ?: ColorDrawable(Color.parseColor("#015A86"))
        }
    }
}