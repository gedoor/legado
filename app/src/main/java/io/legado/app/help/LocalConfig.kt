package io.legado.app.help

import android.content.Context
import androidx.core.content.edit
import io.legado.app.App

object LocalConfig {

    private val localConfig =
        App.INSTANCE.getSharedPreferences("local", Context.MODE_PRIVATE)

    var isFirstOpen: Boolean
        get() = localConfig.getBoolean("firstOpen", true)
        set(value) {
            localConfig.edit { putBoolean("firstOpen", value) }
        }

    var isFirstRead: Boolean
        get() = localConfig.getBoolean("firstRead", true)
        set(value) {
            localConfig.edit { putBoolean("firstRead", value) }
        }

    var isFirstOpenBackup: Boolean
        get() = localConfig.getBoolean("firstBackup", true)
        set(value) {
            localConfig.edit { putBoolean("firstBackup", value) }
        }

}