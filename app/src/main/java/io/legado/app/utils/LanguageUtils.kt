package io.legado.app.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import io.legado.app.constant.PreferKey
import java.util.*


object LanguageUtils {

    /**
     * 设置语言
     */
    fun setConfiguration(context: Context): Context {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val resources: Resources = context.resources
            val targetLocale: Locale = when (context.getPrefString(PreferKey.language)) {
                "zh" -> Locale.CHINESE
                "tw" -> Locale.TRADITIONAL_CHINESE
                "en" -> Locale.ENGLISH
                else -> getSystemLocale()
            }
            val configuration: Configuration = resources.configuration
            configuration.setLocale(targetLocale)
            context.createConfigurationContext(configuration)
        } else {
            context
        }
    }

    /**
     * 设置语言
     */
    fun setConfigurationOld(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            val resources: Resources = context.resources
            val targetLocale: Locale = when (context.getPrefString(PreferKey.language)) {
                "zh" -> Locale.CHINESE
                "tw" -> Locale.TRADITIONAL_CHINESE
                "en" -> Locale.ENGLISH
                else -> getSystemLocale()
            }
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
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { //7.0有多语言设置获取顶部的语言
            Resources.getSystem().configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            Resources.getSystem().configuration.locale
        }
    }


}