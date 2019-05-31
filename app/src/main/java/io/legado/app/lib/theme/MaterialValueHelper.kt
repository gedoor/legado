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