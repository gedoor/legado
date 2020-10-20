@file:Suppress("unused")

package io.legado.app.utils

import android.widget.Toast
import androidx.fragment.app.Fragment
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast


/**
 * Display the simple Toast message with the [Toast.LENGTH_SHORT] duration.
 *
 * @param message the message text resource.
 */
fun Fragment.toast(message: Int) = requireActivity().toast(message)

/**
 * Display the simple Toast message with the [Toast.LENGTH_SHORT] duration.
 *
 * @param message the message text.
 */
fun Fragment.toast(message: CharSequence) = requireActivity().toast(message)

/**
 * Display the simple Toast message with the [Toast.LENGTH_LONG] duration.
 *
 * @param message the message text resource.
 */
fun Fragment.longToast(message: Int) = requireActivity().longToast(message)

/**
 * Display the simple Toast message with the [Toast.LENGTH_LONG] duration.
 *
 * @param message the message text.
 */
fun Fragment.longToast(message: CharSequence) = requireActivity().longToast(message)
