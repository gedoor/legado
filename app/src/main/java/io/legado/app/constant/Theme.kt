package io.legado.app.constant

import io.legado.app.help.AppConfig
import io.legado.app.utils.ColorUtils

enum class Theme {
    Dark, Light, Auto, Transparent, EInk;

    companion object {
        fun getTheme() = when {
            AppConfig.isEInkMode -> EInk
            AppConfig.isNightTheme -> Dark
            else -> Light
        }

        fun getTheme(backgroundColor: Int) =
            if (ColorUtils.isColorLight(backgroundColor)) Light
            else Dark

    }
}