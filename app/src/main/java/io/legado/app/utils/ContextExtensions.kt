package io.legado.app.utils

import android.content.Context
import androidx.core.content.edit
import org.jetbrains.anko.connectivityManager
import org.jetbrains.anko.defaultSharedPreferences

fun Context.isOnline() = connectivityManager.activeNetworkInfo?.isConnected == true

fun Context.getPrefBoolean(key: String, defValue: Boolean) =
    defaultSharedPreferences.getBoolean(key, defValue)

fun Context.putPrefBoolean(key: String, value: Boolean) =
    defaultSharedPreferences.edit { putBoolean(key, value) }


fun Context.getPrefInt(key: String, defValue: Int) =
    defaultSharedPreferences.getInt(key, defValue)

fun Context.putPrefInt(key: String, value: Int) =
    defaultSharedPreferences.edit { putInt(key, value) }

fun Context.getPrefLong(key: String, defValue: Long) =
    defaultSharedPreferences.getLong(key, defValue)

fun Context.putPrefLong(key: String, value: Long) =
    defaultSharedPreferences.edit { putLong(key, value) }

fun Context.getPrefString(key: String, defValue: String) =
    defaultSharedPreferences.getString(key, defValue)

fun Context.putPrefString(key: String, value: String) =
    defaultSharedPreferences.edit { putString(key, value) }

fun Context.getPrefStringSet(key: String, defValue: MutableSet<String>) =
    defaultSharedPreferences.getStringSet(key, defValue)

fun Context.putPrefStringSet(key: String, value: MutableSet<String>) =
    defaultSharedPreferences.edit { putStringSet(key, value) }

fun Context.removePref(key: String) =
    defaultSharedPreferences.edit { remove(key) }

fun Context.getStatusBarHeight(): Int {
    val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
    return resources.getDimensionPixelSize(resourceId)
}
