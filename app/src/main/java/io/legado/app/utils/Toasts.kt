@file:Suppress("unused")

package io.legado.app.utils

import android.widget.Toast
import androidx.fragment.app.Fragment


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
fun Fragment.longToast(message: Int) = requireContext().longToastOnUi(message)

/**
 * Display the simple Toast message with the [Toast.LENGTH_LONG] duration.
 *
 * @param message the message text.
 */
fun Fragment.longToast(message: CharSequence) = requireContext().longToastOnUi(message)
