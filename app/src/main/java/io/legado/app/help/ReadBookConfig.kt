package io.legado.app.help

import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import io.legado.app.App
import io.legado.app.R
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.utils.*
import java.io.File

/**
 * 阅读界面配置
 */
object ReadBookConfig {
    const val readConfigFileName = "readConfig.json"
    private val configFilePath =
        App.INSTANCE.filesDir.absolutePath + File.separator + readConfigFileName
    val configList: ArrayList<Config> = arrayListOf()

    var styleSelect
        get() = App.INSTANCE.getPrefInt("readStyleSelect")
        set(value) = App.INSTANCE.putPrefInt("readStyleSelect", value)
    var bg: Drawable? = null

    init {
        upConfig()
    }

    @Synchronized
    fun getConfig(index: Int = styleSelect): Config {
        if (configList.size < 5) {
            resetAll()
        }
        return configList[index]
    }

    fun upConfig() {
        (getConfigs() ?: getDefaultConfigs()).let {
            configList.clear()
            configList.addAll(it)
        }
    }

    private fun getConfigs(): List<Config>? {
        val configFile = File(configFilePath)
        if (configFile.exists()) {
            try {
                val json = configFile.readText()
                return GSON.fromJsonArray(json)
            } catch (e: Exception) {
            }
        }
        return null
    }

    private fun getDefaultConfigs(): List<Config> {
        val json = String(App.INSTANCE.assets.open(readConfigFileName).readBytes())
        return GSON.fromJsonArray(json)!!
    }

    fun upBg() {
        val resources = App.INSTANCE.resources
        val dm = resources.displayMetrics
        val width = dm.widthPixels
        val height = dm.heightPixels
        bg = getConfig().bgDrawable(width, height)
    }

    fun save() {
        Coroutine.async {
            val json = GSON.toJson(configList)
            FileUtils.createFileIfNotExist(configFilePath).writeText(json)
        }
    }

    fun resetDur() {
        getDefaultConfigs()[styleSelect].let {
            getConfig().setBg(it.bgType(), it.bgStr())
            getConfig().setTextColor(it.textColor())
            upBg()
            save()
        }
    }

    private fun resetAll() {
        getDefaultConfigs().let {
            configList.clear()
            configList.addAll(it)
            save()
        }
    }

    data class Config(
        var bgStr: String = "#EEEEEE",//白天背景
        var bgStrNight: String = "#000000",//夜间背景
        var bgType: Int = 0,//白天背景类型
        var bgTypeNight: Int = 0,//夜间背景类型
        var darkStatusIcon: Boolean = true,//白天是否暗色状态栏
        var darkStatusIconNight: Boolean = false,//晚上是否暗色状态栏
        var textColor: String = "#3E3D3B",//白天文字颜色
        var textColorNight: String = "#adadad",//夜间文字颜色
        var letterSpacing: Float = 1f,//字间距
        var lineSpacingExtra: Int = 12,//行间距
        var lineSpacingMultiplier: Float = 1.2f,//行倍距
        var paddingBottom: Int = 0,
        var paddingLeft: Int = 16,
        var paddingRight: Int = 16,
        var paddingTop: Int = 0,
        var textBold: Boolean = false,//是否粗体字
        var textSize: Int = 15//文字大小
    ) {
        fun setBg(bgType: Int, bg: String) {
            if (AppConfig.isNightTheme) {
                bgTypeNight = bgType
                bgStrNight = bg
            } else {
                this.bgType = bgType
                bgStr = bg
            }
        }

        fun setTextColor(color: Int) {
            if (AppConfig.isNightTheme) {
                textColorNight = "#${color.hexString}"
            } else {
                textColor = "#${color.hexString}"
            }
        }

        fun setStatusIconDark(isDark: Boolean) {
            if (AppConfig.isNightTheme) {
                darkStatusIconNight = isDark
            } else {
                darkStatusIcon = isDark
            }
        }

        fun statusIconDark(): Boolean {
            return if (AppConfig.isNightTheme) {
                darkStatusIconNight
            } else {
                darkStatusIcon
            }
        }

        fun textColor(): Int {
            return if (AppConfig.isNightTheme) Color.parseColor(textColorNight)
            else Color.parseColor(textColor)
        }

        fun bgStr(): String {
            return if (AppConfig.isNightTheme) bgStrNight
            else bgStr
        }

        fun bgType(): Int {
            return if (AppConfig.isNightTheme) bgTypeNight
            else bgType
        }

        fun bgDrawable(width: Int, height: Int): Drawable {
            var bgDrawable: Drawable? = null
            val resources = App.INSTANCE.resources
            try {
                bgDrawable = when (bgType()) {
                    0 -> ColorDrawable(Color.parseColor(bgStr()))
                    1 -> {
                        BitmapDrawable(
                            resources,
                            BitmapUtils.decodeBitmap(
                                App.INSTANCE,
                                "bg" + File.separator + bgStr(),
                                width,
                                height
                            )
                        )
                    }
                    else -> BitmapDrawable(
                        resources,
                        BitmapUtils.decodeBitmap(bgStr(), width, height)
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return bgDrawable ?: ColorDrawable(App.INSTANCE.getCompatColor(R.color.background))
        }
    }
}