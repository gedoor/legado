package io.legado.app.ui.config

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class ConfigViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val TYPE_CONFIG = 0
        const val TYPE_THEME_CONFIG = 1
        const val TYPE_WEB_DAV_CONFIG = 2
    }

    var configType: Int = TYPE_CONFIG

}