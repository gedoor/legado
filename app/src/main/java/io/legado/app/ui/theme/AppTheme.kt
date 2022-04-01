package io.legado.app.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.legado.app.help.config.ThemeConfig
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.primaryColor
import io.legado.app.utils.ColorUtils
import splitties.init.appCtx

object AppTheme {

    val colors
        get() = if (ThemeConfig.isDarkTheme()) {
            darkColors(
                primary = Color(appCtx.primaryColor),
                primaryVariant = Color(ColorUtils.darkenColor(appCtx.primaryColor)),
                secondary = Color(appCtx.accentColor),
                secondaryVariant = Color(appCtx.accentColor)
            )
        } else {
            lightColors(
                primary = Color(appCtx.primaryColor),
                primaryVariant = Color(ColorUtils.darkenColor(appCtx.primaryColor)),
                secondary = Color(appCtx.accentColor),
                secondaryVariant = Color(appCtx.accentColor)
            )
        }

}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = AppTheme.colors,
        content = content
    )
}