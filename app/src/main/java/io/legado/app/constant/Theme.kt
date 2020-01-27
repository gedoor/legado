package io.legado.app.constant

import io.legado.app.App
import io.legado.app.help.isNightTheme

enum class Theme {
    Dark, Light, Auto;

    companion object {
        fun getTheme(): Theme {
            return if (App.INSTANCE.isNightTheme) {
                Dark
            } else Light
        }
    }
}