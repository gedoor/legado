package io.legado.app.lib.theme

/**
 * @author Aidan Follestad (afollestad), Karim Abou Zeid (kabouzeid)
 */
internal interface ThemeStorePrefKeys {
    companion object {

        val CONFIG_PREFS_KEY_DEFAULT = "app_themes"
        val IS_CONFIGURED_KEY = "is_configured"
        val IS_CONFIGURED_VERSION_KEY = "is_configured_version"
        val VALUES_CHANGED = "values_changed"

        val KEY_PRIMARY_COLOR = "primary_color"
        val KEY_PRIMARY_COLOR_DARK = "primary_color_dark"
        val KEY_ACCENT_COLOR = "accent_color"
        val KEY_STATUS_BAR_COLOR = "status_bar_color"
        val KEY_NAVIGATION_BAR_COLOR = "navigation_bar_color"

        val KEY_TEXT_COLOR_PRIMARY = "text_color_primary"
        val KEY_TEXT_COLOR_PRIMARY_INVERSE = "text_color_primary_inverse"
        val KEY_TEXT_COLOR_SECONDARY = "text_color_secondary"
        val KEY_TEXT_COLOR_SECONDARY_INVERSE = "text_color_secondary_inverse"

        val KEY_BACKGROUND_COLOR = "backgroundColor"

        val KEY_APPLY_PRIMARYDARK_STATUSBAR = "apply_primarydark_statusbar"
        val KEY_APPLY_PRIMARY_NAVBAR = "apply_primary_navbar"
        val KEY_AUTO_GENERATE_PRIMARYDARK = "auto_generate_primarydark"
    }
}