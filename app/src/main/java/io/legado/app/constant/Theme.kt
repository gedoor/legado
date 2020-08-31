package io.legado.app.constant

import io.legado.app.help.AppConfig
import io.legado.app.utils.ColorUtils

enum class Theme {
    Dark, Light, Auto, Transparent;

    companion object {
        fun getTheme(): Theme {
            return if (AppConfig.isNightTheme) {
                Dark
            } else Light
        }

        fun getTheme(backgroundColor: Int): Theme {
            return if (ColorUtils.isColorLight(backgroundColor)) {
                Light
            } else {
                Dark
            }
        }
    }
}