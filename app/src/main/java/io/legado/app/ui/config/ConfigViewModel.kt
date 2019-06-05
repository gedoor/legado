package io.legado.app.ui.config

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class ConfigViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        val TYPE_CONFIG = 0
        val TYPE_THEME_CONFIG = 1
        val TYPE_WEBDAV_CONFIG = 2
    }

    var configType: Int = TYPE_CONFIG

}