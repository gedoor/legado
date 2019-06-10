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


@ColorInt
fun Context.getPrimaryTextColor(): Int {
    return getPrimaryTextColor(isDarkTheme())
}

@ColorInt
fun Context.getSecondaryTextColor(): Int {
    return getSecondaryTextColor(isDarkTheme())
}

@ColorInt
fun Context.getPrimaryDisabledTextColor(): Int {
    return getPrimaryDisabledTextColor(isDarkTheme())
}

@ColorInt
fun Context.getSecondaryDisabledTextColor(): Int {
    return getSecondaryDisabledTextColor(isDarkTheme())
}


@ColorInt
fun Fragment.getPrimaryTextColor(): Int {
    return getPrimaryTextColor(isDarkTheme())
}

@ColorInt
fun Fragment.getSecondaryTextColor(): Int {
    return getSecondaryTextColor(isDarkTheme())
}

@ColorInt
fun Fragment.getPrimaryDisabledTextColor(): Int {
    return getPrimaryDisabledTextColor(isDarkTheme())
}

@ColorInt
fun Fragment.getSecondaryDisabledTextColor(): Int {
    return getSecondaryDisabledTextColor(isDarkTheme())
}

fun Context.isDarkTheme(): Boolean {
    return ColorUtils.isColorLight(ThemeStore.primaryColor(this))
}

fun Fragment.isDarkTheme(): Boolean {
    return requireContext().isDarkTheme()
}