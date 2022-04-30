package io.legado.app.utils

import android.content.SharedPreferences
import androidx.core.content.edit


fun SharedPreferences.putLong(key: String, value: Long) {
    edit {
        putLong(key, value)
    }
}