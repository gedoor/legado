@file:Suppress("unused")

package io.legado.app.utils

import android.widget.Toast
import androidx.fragment.app.Fragment
import org.jetbrains.anko.longToast


/**
 * Display the simple Toast message with the [Toast.LENGTH_SHORT] duration.
 *
 * @param message the message text resource.
 */
fun Fragment.toastOnUi(message: Int) = requireActivity().toastOnUi(message)

/**
 * Display the simple Toast message with the [Toast.LENGTH_SHORT] duration.
 *
 * @param message the message text.
 */
fun Fragment.toastOnUi(message: CharSequence) = requireActivity().toastOnUi(message)

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
