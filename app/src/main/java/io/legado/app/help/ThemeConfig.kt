package io.legado.app.help

import android.content.Context
import android.graphics.Color
import io.legado.app.App
import io.legado.app.R
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.utils.*

object ThemeConfig {
    val configFileName = "themeConfigs.json"
    private val configFilePath = FileUtils.getPath(App.INSTANCE.filesDir, configFileName)
    val configList = arrayListOf<Config>()
    private val defaultConfigs by lazy {
        val json = String(App.INSTANCE.assets.open(configFileName).readBytes())
        GSON.fromJsonArray<Config>(json)!!
    }


    fun applyConfig(context: Context, config: Config) {
        val primary = Color.parseColor(config.primaryColor)
        val accent = Color.parseColor(config.accentColor)
        var background = Color.parseColor(config.backgroundColor)
        if (!ColorUtils.isColorLight(background)) {
            background = context.getCompatColor(R.color.md_grey_100)
        }
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

    class Config(
        var configName: String = "典雅蓝",
        var isNightTheme: Boolean = false,
        var primaryColor: String = "#03A9F4",
        var accentColor: String = "#AD1457",
        var backgroundColor: String = "#F5F5F5",
        var bottomBackground: String = "#EEEEEE"
    )

}