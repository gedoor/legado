@file:Suppress("unused")

package io.legado.app.utils

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.Fragment

private var toast: Toast? = null

fun Context.toastOnUi(message: Int) {
    runOnUI {
        if (toast == null) {
            toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        } else {
            toast?.setText(message)
            toast?.duration = Toast.LENGTH_SHORT
        }
        toast?.show()
    }
}

fun Context.toastOnUi(message: CharSequence?) {
    runOnUI {
        if (toast == null) {
            toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        } else {
            toast?.setText(message)
            toast?.duration = Toast.LENGTH_SHORT
        }
        toast?.show()
    }
}

fun Context.longToastOnUi(message: Int) {
    runOnUI {
        if (toast == null) {
            toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
        } else {
            toast?.setText(message)
            toast?.duration = Toast.LENGTH_LONG
        }
        toast?.show()
    }
}

fun Context.longToastOnUi(message: CharSequence?) {
    runOnUI {
        if (toast == null) {
            toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
        } else {
            toast?.setText(message)
            toast?.duration = Toast.LENGTH_LONG
        }
        toast?.show()
    }
}


fun Fragment.toastOnUi(message: Int) = requireActivity().toastOnUi(message)

fun Fragment.toastOnUi(message: CharSequence) = requireActivity().toastOnUi(message)

fun Fragment.longToast(message: Int) = requireContext().longToastOnUi(message)

fun Fragment.longToast(message: CharSequence) = requireContext().longToastOnUi(message)
