@file:Suppress("unused")

package io.legado.app.utils

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment

inline fun <reified T : DialogFragment> Fragment.showDialogFragment(
    arguments: Bundle.() -> Unit = {}
) {
    @Suppress("DEPRECATION")
    val dialog = T::class.java.newInstance()
    val bundle = Bundle()
    bundle.apply(arguments)
    dialog.arguments = bundle
    dialog.show(childFragmentManager, T::class.simpleName)
}

fun Fragment.showDialogFragment(dialogFragment: DialogFragment) {
    dialogFragment.show(childFragmentManager, dialogFragment::class.simpleName)
}

fun Fragment.getPrefBoolean(key: String, defValue: Boolean = false) =
    requireContext().defaultSharedPreferences.getBoolean(key, defValue)

fun Fragment.putPrefBoolean(key: String, value: Boolean = false) =
    requireContext().defaultSharedPreferences.edit { putBoolean(key, value) }

fun Fragment.getPrefInt(key: String, defValue: Int = 0) =
    requireContext().defaultSharedPreferences.getInt(key, defValue)

fun Fragment.putPrefInt(key: String, value: Int) =
    requireContext().defaultSharedPreferences.edit { putInt(key, value) }

fun Fragment.getPrefLong(key: String, defValue: Long = 0L) =
    requireContext().defaultSharedPreferences.getLong(key, defValue)

fun Fragment.putPrefLong(key: String, value: Long) =
    requireContext().defaultSharedPreferences.edit { putLong(key, value) }

fun Fragment.getPrefString(key: String, defValue: String? = null) =
    requireContext().defaultSharedPreferences.getString(key, defValue)

fun Fragment.putPrefString(key: String, value: String) =
    requireContext().defaultSharedPreferences.edit { putString(key, value) }

fun Fragment.getPrefStringSet(
    key: String,
    defValue: MutableSet<String>? = null
): MutableSet<String>? =
    requireContext().defaultSharedPreferences.getStringSet(key, defValue)

fun Fragment.putPrefStringSet(key: String, value: MutableSet<String>) =
    requireContext().defaultSharedPreferences.edit { putStringSet(key, value) }

fun Fragment.removePref(key: String) =
    requireContext().defaultSharedPreferences.edit { remove(key) }

fun Fragment.getCompatColor(@ColorRes id: Int): Int = requireContext().getCompatColor(id)

fun Fragment.getCompatDrawable(@DrawableRes id: Int): Drawable? =
    requireContext().getCompatDrawable(id)

fun Fragment.getCompatColorStateList(@ColorRes id: Int): ColorStateList? =
    requireContext().getCompatColorStateList(id)

inline fun <reified T : Activity> Fragment.startActivity(
    configIntent: Intent.() -> Unit = {}
) {
    startActivity(Intent(requireContext(), T::class.java).apply(configIntent))
}