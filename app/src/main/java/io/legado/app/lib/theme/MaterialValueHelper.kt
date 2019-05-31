package io.legado.app.lib.theme

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import io.legado.app.R

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object MaterialValueHelper {

    @SuppressLint("PrivateResource")
    @ColorInt
    fun getPrimaryTextColor(context: Context, dark: Boolean): Int {
        return if (dark) {
            ContextCompat.getColor(context, R.color.primary_text_default_material_light)
        } else ContextCompat.getColor(context, R.color.primary_text_default_material_dark)
    }

    @SuppressLint("PrivateResource")
    @ColorInt
    fun getSecondaryTextColor(context: Context, dark: Boolean): Int {
        return if (dark) {
            ContextCompat.getColor(context, R.color.secondary_text_default_material_light)
        } else ContextCompat.getColor(context, R.color.secondary_text_default_material_dark)
    }

    @SuppressLint("PrivateResource")
    @ColorInt
    fun getPrimaryDisabledTextColor(context: Context, dark: Boolean): Int {
        return if (dark) {
            ContextCompat.getColor(context, R.color.primary_text_disabled_material_light)
        } else ContextCompat.getColor(context, R.color.primary_text_disabled_material_dark)
    }

    @SuppressLint("PrivateResource")
    @ColorInt
    fun getSecondaryDisabledTextColor(context: Context, dark: Boolean): Int {
        return if (dark) {
            ContextCompat.getColor(context, R.color.secondary_text_disabled_material_light)
        } else ContextCompat.getColor(context, R.color.secondary_text_disabled_material_dark)
    }
}
