package io.legado.app.help

import android.content.Context
import androidx.core.content.edit
import io.legado.app.App

object LocalConfig {
    private const val versionCodeKey = "versionCode"

    private val localConfig =
        App.INSTANCE.getSharedPreferences("local", Context.MODE_PRIVATE)

    var versionCode
        get() = localConfig.getInt(versionCodeKey, 0)
        set(value) {
            localConfig.edit {
                putInt(versionCodeKey, value)
            }
        }

    val isFirstOpenApp: Boolean
        get() {
            val value = localConfig.getBoolean("firstOpen", true)
            if (value) {
                localConfig.edit { putBoolean("firstOpen", false) }
            }
            return value
        }

    val isFirstRead: Boolean
        get() {
            val value = localConfig.getBoolean("firstRead", true)
            if (value) {
                localConfig.edit { putBoolean("firstRead", false) }
            }
            return value
        }

    val isFirstOpenBackup: Boolean
        get() {
            val value = localConfig.getBoolean("firstBackup", true)
            if (value) {
                localConfig.edit { putBoolean("firstBackup", false) }
            }
            return value
        }

    val isFirstReadMenuShow: Boolean
        get() {
            val value = localConfig.getBoolean("firstReadMenu", true)
            if (value) {
                localConfig.edit { putBoolean("firstReadMenu", false) }
            }
            return value
        }

    val isFirstOpenBookSources: Boolean
        get() {
            val value = localConfig.getBoolean("firstOpenBookSources", true)
            if (value) {
                localConfig.edit { putBoolean("firstOpenBookSources", false) }
            }
            return value
        }
}