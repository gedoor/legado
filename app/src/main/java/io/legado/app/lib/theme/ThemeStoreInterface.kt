package io.legado.app.lib.theme


import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes

/**
 * @author Aidan Follestad (afollestad), Karim Abou Zeid (kabouzeid)
 */
internal interface ThemeStoreInterface {

    // Primary colors

    fun primaryColor(@ColorInt color: Int): ThemeStore

    fun primaryColorRes(@ColorRes colorRes: Int): ThemeStore

    fun primaryColorAttr(@AttrRes colorAttr: Int): ThemeStore

    fun autoGeneratePrimaryDark(autoGenerate: Boolean): ThemeStore

    fun primaryColorDark(@ColorInt color: Int): ThemeStore

    fun primaryColorDarkRes(@ColorRes colorRes: Int): ThemeStore

    fun primaryColorDarkAttr(@AttrRes colorAttr: Int): ThemeStore

    // Accent colors

    fun accentColor(@ColorInt color: Int): ThemeStore

    fun accentColorRes(@ColorRes colorRes: Int): ThemeStore

    fun accentColorAttr(@AttrRes colorAttr: Int): ThemeStore

    // Status bar color

    fun statusBarColor(@ColorInt color: Int): ThemeStore

    fun statusBarColorRes(@ColorRes colorRes: Int): ThemeStore

    fun statusBarColorAttr(@AttrRes colorAttr: Int): ThemeStore

    // Navigation bar color

    fun navigationBarColor(@ColorInt color: Int): ThemeStore

    fun navigationBarColorRes(@ColorRes colorRes: Int): ThemeStore

    fun navigationBarColorAttr(@AttrRes colorAttr: Int): ThemeStore

    // Primary text color

    fun textColorPrimary(@ColorInt color: Int): ThemeStore

    fun textColorPrimaryRes(@ColorRes colorRes: Int): ThemeStore

    fun textColorPrimaryAttr(@AttrRes colorAttr: Int): ThemeStore

    fun textColorPrimaryInverse(@ColorInt color: Int): ThemeStore

    fun textColorPrimaryInverseRes(@ColorRes colorRes: Int): ThemeStore

    fun textColorPrimaryInverseAttr(@AttrRes colorAttr: Int): ThemeStore

    // Secondary text color

    fun textColorSecondary(@ColorInt color: Int): ThemeStore

    fun textColorSecondaryRes(@ColorRes colorRes: Int): ThemeStore

    fun textColorSecondaryAttr(@AttrRes colorAttr: Int): ThemeStore

    fun textColorSecondaryInverse(@ColorInt color: Int): ThemeStore

    fun textColorSecondaryInverseRes(@ColorRes colorRes: Int): ThemeStore

    fun textColorSecondaryInverseAttr(@AttrRes colorAttr: Int): ThemeStore

    // Background

    fun backgroundColor(@ColorInt color: Int): ThemeStore

    fun bottomBackground(@ColorInt color: Int): ThemeStore

    // Commit/apply

    fun apply()
}
