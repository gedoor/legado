package io.legado.app.utils

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import org.jetbrains.anko.dip
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast


/**
 * Display the Snackbar with the [Snackbar.LENGTH_SHORT] duration.
 *
 * @param message the message text resource.
 */
@JvmName("snackbar2")
fun View.snackbar(@StringRes message: Int) = Snackbar
    .make(this, message, Snackbar.LENGTH_SHORT)
    .apply { show() }

/**
 * Display Snackbar with the [Snackbar.LENGTH_LONG] duration.
 *
 * @param message the message text resource.
 */
@JvmName("longSnackbar2")
fun View.longSnackbar(@StringRes message: Int) = Snackbar
    .make(this, message, Snackbar.LENGTH_LONG)
    .apply { show() }

/**
 * Display Snackbar with the [Snackbar.LENGTH_LONG] duration.
 *
 * @param message the message text resource.
 */
@JvmName("longSnackbar2")
fun View.longSnackbar(@StringRes message: Int, @StringRes actionText: Int, action: (View) -> Unit) = Snackbar
    .make(this, message, Snackbar.LENGTH_LONG)
    .setAction(actionText, action)
    .apply { show() }

fun Fragment.toast(textResource: Int) = requireActivity().toast(textResource)

fun Fragment.toast(text: CharSequence) = requireActivity().toast(text)

fun Fragment.longToast(textResource: Int) = requireActivity().longToast(textResource)

fun Fragment.longToast(text: CharSequence) = requireActivity().longToast(text)

fun Fragment.dip(value: Int): Int = requireActivity().dip(value)

fun Fragment.dip(value: Float): Int = requireActivity().dip(value)

fun Fragment.getCompatColor(@ColorRes id: Int): Int = requireContext().getCompatColor(id)

fun Fragment.getCompatDrawable(@DrawableRes id: Int): Drawable? = requireContext().getCompatDrawable(id)

fun Fragment.getCompatColorStateList(@ColorRes id: Int): ColorStateList? = requireContext().getCompatColorStateList(id)