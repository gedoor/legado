package io.legado.app.lib.theme

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import io.legado.app.R

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
@SuppressLint("PrivateResource")
@ColorInt
fun Context.getPrimaryTextColor(dark: Boolean): Int {
    return if (dark) {
        ContextCompat.getColor(this, R.color.primary_text_default_material_light)
    } else ContextCompat.getColor(this, R.color.primary_text_default_material_dark)
}

@SuppressLint("PrivateResource")
@ColorInt
fun Context.getSecondaryTextColor(dark: Boolean): Int {
    return if (dark) {
        ContextCompat.getColor(this, R.color.secondary_text_default_material_light)
    } else ContextCompat.getColor(this, R.color.secondary_text_default_material_dark)
}

@SuppressLint("PrivateResource")
@ColorInt
fun Context.getPrimaryDisabledTextColor(dark: Boolean): Int {
    return if (dark) {
        ContextCompat.getColor(this, R.color.primary_text_disabled_material_light)
    } else ContextCompat.getColor(this, R.color.primary_text_disabled_material_dark)
}

@SuppressLint("PrivateResource")
@ColorInt
fun Context.getSecondaryDisabledTextColor(dark: Boolean): Int {
    return if (dark) {
        ContextCompat.getColor(this, R.color.secondary_text_disabled_material_light)
    } else ContextCompat.getColor(this, R.color.secondary_text_disabled_material_dark)
}


@SuppressLint("PrivateResource")
@ColorInt
fun Fragment.getPrimaryTextColor(dark: Boolean): Int {
    return if (dark) {
        ContextCompat.getColor(requireContext(), R.color.primary_text_default_material_light)
    } else ContextCompat.getColor(requireContext(), R.color.primary_text_default_material_dark)
}

@SuppressLint("PrivateResource")
@ColorInt
fun Fragment.getSecondaryTextColor(dark: Boolean): Int {
    return if (dark) {
        ContextCompat.getColor(requireContext(), R.color.secondary_text_default_material_light)
    } else ContextCompat.getColor(requireContext(), R.color.secondary_text_default_material_dark)
}

@SuppressLint("PrivateResource")
@ColorInt
fun Fragment.getPrimaryDisabledTextColor(dark: Boolean): Int {
    return if (dark) {
        ContextCompat.getColor(requireContext(), R.color.primary_text_disabled_material_light)
    } else ContextCompat.getColor(requireContext(), R.color.primary_text_disabled_material_dark)
}

@SuppressLint("PrivateResource")
@ColorInt
fun Fragment.getSecondaryDisabledTextColor(dark: Boolean): Int {
    return if (dark) {
        ContextCompat.getColor(requireContext(), R.color.secondary_text_disabled_material_light)
    } else ContextCompat.getColor(requireContext(), R.color.secondary_text_disabled_material_dark)
}

val Context.primaryColor: Int
    get() = ThemeStore.primaryColor(this)

val Context.primaryColorDark: Int
    get() = ThemeStore.primaryColorDark(this)

val Context.accentColor: Int
    get() = ThemeStore.accentColor(this)

val Context.backgroundColor: Int
    get() = ThemeStore.backgroundColor(this)

val Context.primaryTextColor: Int
    get() = getPrimaryTextColor(isDarkTheme)

val Context.secondaryTextColor: Int
    get() = getSecondaryTextColor(isDarkTheme)

val Context.primaryDisabledTextColor: Int
    get() = getPrimaryDisabledTextColor(isDarkTheme)

val Context.secondaryDisabledTextColor: Int
    get() = getSecondaryDisabledTextColor(isDarkTheme)

val Fragment.primaryColor: Int
    get() = ThemeStore.primaryColor(requireContext())

val Fragment.primaryColorDark: Int
    get() = ThemeStore.primaryColorDark(requireContext())

val Fragment.accentColor: Int
    get() = ThemeStore.accentColor(requireContext())

val Fragment.backgroundColor: Int
    get() = ThemeStore.backgroundColor(requireContext())

val Fragment.primaryTextColor: Int
    get() = getPrimaryTextColor(isDarkTheme)

val Fragment.secondaryTextColor: Int
    get() = getSecondaryTextColor(isDarkTheme)

val Fragment.primaryDisabledTextColor: Int
    get() = getPrimaryDisabledTextColor(isDarkTheme)

val Fragment.secondaryDisabledTextColor: Int
    get() = getSecondaryDisabledTextColor(isDarkTheme)

val Context.isDarkTheme: Boolean
    get() = ColorUtils.isColorLight(ThemeStore.primaryColor(this))

val Fragment.isDarkTheme: Boolean
    get() = requireContext().isDarkTheme