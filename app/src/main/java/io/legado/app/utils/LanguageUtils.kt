package io.legado.app.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import io.legado.app.constant.PreferKey
import java.util.*


object LanguageUtils {

    /**
     * 设置语言
     */
    fun setConfiguration(context: Context): Context {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val resources: Resources = context.resources
            val metrics = resources.displayMetrics
            val configuration: Configuration = resources.configuration
            //Log.d("h11128", "set language to ${context.getPrefString(PreferKey.language)}")
            val targetLocale = getSetLocale(context)
            configuration.setLocale(targetLocale)
            configuration.setLocales(LocaleList(targetLocale))
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, metrics)
            context.createConfigurationContext(configuration)
        } else {
            setConfigurationOld(context)
            context
        }
    }

    /**
     * 设置语言
     */
    private fun setConfigurationOld(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            val resources: Resources = context.resources
            val targetLocale = getSetLocale(context)
            val configuration: Configuration = resources.configuration
            @Suppress("DEPRECATION")
            configuration.locale = targetLocale
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, resources.displayMetrics)
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