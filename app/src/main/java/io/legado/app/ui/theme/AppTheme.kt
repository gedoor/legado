package io.legado.app.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.legado.app.help.config.ThemeConfig
import io.legado.app.lib.theme.primaryColor
import splitties.init.appCtx


@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val colors = if (ThemeConfig.isDarkTheme()) {
        darkColors(
            primary = Color(appCtx.primaryColor),
        )
    } else {
        lightColors(
            primary = Color(appCtx.primaryColor),
        )
    }
    MaterialTheme(
        colors = colors,
        content = content
    )
}