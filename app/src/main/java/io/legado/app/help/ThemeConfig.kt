package io.legado.app.help

import android.content.Context
import android.graphics.Color
import io.legado.app.App
import io.legado.app.R
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.utils.*
import java.io.File

object ThemeConfig {
    const val configFileName = "themeConfig.json"
    val configFilePath = FileUtils.getPath(App.INSTANCE.filesDir, configFileName)
    private val defaultConfigs by lazy {
        val json = String(App.INSTANCE.assets.open(configFileName).readBytes())
        GSON.fromJsonArray<Config>(json)!!
    }
    val configList = arrayListOf<Config>()

    init {
        upConfig()
    }

    fun upConfig() {
        (getConfigs() ?: defaultConfigs).let {
            configList.clear()
            configList.addAll(it)
        }
    }

    fun save() {
        Coroutine.async {
            synchronized(this) {
                val json = GSON.toJson(configList)
                FileUtils.deleteFile(configFilePath)
                FileUtils.createFileIfNotExist(configFilePath).writeText(json)
            }
        }
    }

    fun addConfig(newConfig: Config) {
        configList.forEachIndexed { index, config ->
            if (newConfig.themeName == config.themeName) {
                configList[index] = newConfig
                return
            }
        }
        configList.add(newConfig)
    }

    private fun getConfigs(): List<Config>? {
        val configFile = File(configFilePath)
        if (configFile.exists()) {
            try {
                val json = configFile.readText()
                return GSON.fromJsonArray(json)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    fun applyConfig(context: Context, config: Config) {
        val primary = Color.parseColor(config.primaryColor)
        val accent = Color.parseColor(config.accentColor)
        val background = Color.parseColor(config.backgroundColor)
        val bBackground = Color.parseColor(config.bottomBackground)
        if (config.isNightTheme) {
            context.putPrefInt(PreferKey.cNPrimary, primary)
            context.putPrefInt(PreferKey.cNAccent, accent)
            context.putPrefInt(PreferKey.cNBackground, background)
            context.putPrefInt(PreferKey.cNBBackground, bBackground)
        } else {
            context.putPrefInt(PreferKey.cPrimary, primary)
            context.putPrefInt(PreferKey.cAccent, accent)
            context.putPrefInt(PreferKey.cBackground, background)
            context.putPrefInt(PreferKey.cBBackground, bBackground)
        }
        AppConfig.isNightTheme = config.isNightTheme
        App.INSTANCE.applyDayNight()
        postEvent(EventBus.RECREATE, "")
    }

    fun saveDayTheme(context: Context, name: String) {
        val primary =
            context.getPrefInt(PreferKey.cPrimary, context.getCompatColor(R.color.md_brown_500))
        val accent =
            context.getPrefInt(PreferKey.cAccent, context.getCompatColor(R.color.md_red_600))
        val background =
            context.getPrefInt(PreferKey.cBackground, context.getCompatColor(R.color.md_grey_100))
        val bBackground =
            context.getPrefInt(PreferKey.cBBackground, context.getCompatColor(R.color.md_grey_200))
        val config = Config(
            themeName = name,
            isNightTheme = false,
            primaryColor = "#${primary.hexString}",
            accentColor = "#${accent.hexString}",
            backgroundColor = "#${background.hexString}",
            bottomBackground = "#${bBackground.hexString}"
        )
        addConfig(config)
    }

    fun saveNightTheme(context: Context, name: String) {
        val primary =
            context.getPrefInt(PreferKey.cNPrimary, context.getCompatColor(R.color.md_blue_grey_600))
        val accent =
            context.getPrefInt(PreferKey.cNAccent, context.getCompatColor(R.color.md_deep_orange_800))
        val background =
            context.getPrefInt(PreferKey.cNBackground, context.getCompatColor(R.color.md_grey_900))
        val bBackground =
            context.getPrefInt(PreferKey.cNBBackground, context.getCompatColor(R.color.md_grey_850))
        val config = Config(
            themeName = name,
            isNightTheme = true,
            primaryColor = "#${primary.hexString}",
            accentColor = "#${accent.hexString}",
            backgroundColor = "#${background.hexString}",
            bottomBackground = "#${bBackground.hexString}"
        )
        addConfig(config)
    }

    class Config(
        var themeName: String = "典雅蓝",
        var isNightTheme: Boolean = false,
        var primaryColor: String = "#03A9F4",
        var accentColor: String = "#AD1457",
        var backgroundColor: String = "#F5F5F5",
        var bottomBackground: String = "#EEEEEE"
    )

}