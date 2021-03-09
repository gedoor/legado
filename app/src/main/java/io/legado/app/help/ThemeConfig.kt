package io.legado.app.help

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatDelegate
import io.legado.app.R
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.constant.Theme
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.utils.*
import splitties.init.appCtx
import java.io.File

object ThemeConfig {
    const val configFileName = "themeConfig.json"
    val configFilePath = FileUtils.getPath(appCtx.filesDir, configFileName)

    val configList: ArrayList<Config> by lazy {
        val cList = getConfigs() ?: DefaultData.themeConfigs
        ArrayList(cList)
    }

    fun applyDayNight(context: Context) {
        ReadBookConfig.upBg()
        applyTheme(context)
        initNightMode()
        postEvent(EventBus.RECREATE, "")
    }

    private fun initNightMode() {
        val targetMode =
            if (AppConfig.isNightTheme) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        AppCompatDelegate.setDefaultNightMode(targetMode)
    }

    fun getBgImage(context: Context): Drawable? {
        val bgPath = when (Theme.getTheme()) {
            Theme.Light -> context.getPrefString(PreferKey.bgImage)
            Theme.Dark -> context.getPrefString(PreferKey.bgImageN)
            else -> null
        }
        if (bgPath.isNullOrBlank()) return null
        return BitmapDrawable.createFromPath(bgPath)
    }

    fun upConfig() {
        getConfigs()?.forEach { config ->
            addConfig(config)
        }
    }

    fun save() {
        val json = GSON.toJson(configList)
        FileUtils.deleteFile(configFilePath)
        FileUtils.createFileIfNotExist(configFilePath).writeText(json)
    }

    fun delConfig(index: Int) {
        configList.removeAt(index)
        save()
    }

    fun addConfig(json: String): Boolean {
        GSON.fromJsonObject<Config>(json.trim { it < ' ' })?.let {
            addConfig(it)
            return true
        }
        return false
    }

    private fun addConfig(newConfig: Config) {
        configList.forEachIndexed { index, config ->
            if (newConfig.themeName == config.themeName) {
                configList[index] = newConfig
                return
            }
        }
        configList.add(newConfig)
        save()
    }

    private fun getConfigs(): List<Config>? {
        val configFile = File(configFilePath)
        if (configFile.exists()) {
            kotlin.runCatching {
                val json = configFile.readText()
                return GSON.fromJsonArray(json)
            }.onFailure {
                it.printStackTrace()
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
        applyDayNight(context)
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
            context.getPrefInt(
                PreferKey.cNPrimary,
                context.getCompatColor(R.color.md_blue_grey_600)
            )
        val accent =
            context.getPrefInt(
                PreferKey.cNAccent,
                context.getCompatColor(R.color.md_deep_orange_800)
            )
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

    /**
     * 更新主题
     */
    fun applyTheme(context: Context) = with(context) {
        when {
            AppConfig.isEInkMode -> {
                ThemeStore.editTheme(this)
                    .coloredNavigationBar(true)
                    .primaryColor(Color.WHITE)
                    .accentColor(Color.BLACK)
                    .backgroundColor(Color.WHITE)
                    .bottomBackground(Color.WHITE)
                    .apply()
            }
            AppConfig.isNightTheme -> {
                val primary =
                    getPrefInt(PreferKey.cNPrimary, getCompatColor(R.color.md_blue_grey_600))
                val accent =
                    getPrefInt(PreferKey.cNAccent, getCompatColor(R.color.md_deep_orange_800))
                var background =
                    getPrefInt(PreferKey.cNBackground, getCompatColor(R.color.md_grey_900))
                if (ColorUtils.isColorLight(background)) {
                    background = getCompatColor(R.color.md_grey_900)
                    putPrefInt(PreferKey.cNBackground, background)
                }
                val bBackground =
                    getPrefInt(PreferKey.cNBBackground, getCompatColor(R.color.md_grey_850))
                ThemeStore.editTheme(this)
                    .coloredNavigationBar(true)
                    .primaryColor(ColorUtils.withAlpha(primary, 1f))
                    .accentColor(ColorUtils.withAlpha(accent, 1f))
                    .backgroundColor(ColorUtils.withAlpha(background, 1f))
                    .bottomBackground(ColorUtils.withAlpha(bBackground, 1f))
                    .apply()
            }
            else -> {
                val primary =
                    getPrefInt(PreferKey.cPrimary, getCompatColor(R.color.md_brown_500))
                val accent =
                    getPrefInt(PreferKey.cAccent, getCompatColor(R.color.md_red_600))
                var background =
                    getPrefInt(PreferKey.cBackground, getCompatColor(R.color.md_grey_100))
                if (!ColorUtils.isColorLight(background)) {
                    background = getCompatColor(R.color.md_grey_100)
                    putPrefInt(PreferKey.cBackground, background)
                }
                val bBackground =
                    getPrefInt(PreferKey.cBBackground, getCompatColor(R.color.md_grey_200))
                ThemeStore.editTheme(this)
                    .coloredNavigationBar(true)
                    .primaryColor(ColorUtils.withAlpha(primary, 1f))
                    .accentColor(ColorUtils.withAlpha(accent, 1f))
                    .backgroundColor(ColorUtils.withAlpha(background, 1f))
                    .bottomBackground(ColorUtils.withAlpha(bBackground, 1f))
                    .apply()
            }
        }
    }

    @Keep
    class Config(
        var themeName: String,
        var isNightTheme: Boolean,
        var primaryColor: String,
        var accentColor: String,
        var backgroundColor: String,
        var bottomBackground: String
    )

}