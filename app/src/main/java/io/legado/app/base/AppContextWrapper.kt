package io.legado.app.base

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import io.legado.app.constant.PreferKey
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.getPrefString
import java.util.*


object AppContextWrapper {

    fun wrap(context: Context): Context {
        var fontScale = context.getPrefInt(PreferKey.fontScale) / 10f
        if (fontScale !in 1f..2f) {
            fontScale = Configuration().fontScale
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val resources: Resources = context.resources
            val configuration: Configuration = resources.configuration
            val targetLocale = getSetLocale(context)
            configuration.setLocale(targetLocale)
            configuration.setLocales(LocaleList(targetLocale))
            configuration.fontScale = fontScale
            context.createConfigurationContext(configuration)
        } else {
            val resources: Resources = context.resources
            val targetLocale = getSetLocale(context)
            val configuration: Configuration = resources.configuration
            @Suppress("DEPRECATION")
            configuration.locale = targetLocale
            configuration.fontScale = fontScale
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, resources.displayMetrics)
            context
        }
    }

    /**
     * 当前系统语言
     */
    private fun getSystemLocale(): Locale {
        val locale: Locale
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { //7.0有多语言设置获取顶部的语言
            locale = Resources.getSystem().configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            locale = Resources.getSystem().configuration.locale
        }
        return locale
    }

    /**
     * 当前App语言
     */
    private fun getAppLocale(context: Context): Locale {
        val locale: Locale
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            locale = context.resources.configuration.locale
        }
        return locale

    }

    /**
     * 当前设置语言
     */
    private fun getSetLocale(context: Context): Locale {
        return when (context.getPrefString(PreferKey.language)) {
            "zh" -> Locale.SIMPLIFIED_CHINESE
            "tw" -> Locale.TRADITIONAL_CHINESE
            "en" -> Locale.ENGLISH
            else -> getSystemLocale()
        }
    }

    /**
     * 判断App语言和设置语言是否相同
     */
    fun isSameWithSetting(context: Context): Boolean {
        val locale = getAppLocale(context)
        val language = locale.language
        val country = locale.country
        val pfLocale = getSetLocale(context)
        val pfLanguage = pfLocale.language
        val pfCountry = pfLocale.country
        return language == pfLanguage && country == pfCountry
    }

}