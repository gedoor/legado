package io.legado.app.ui.config

import android.app.Application
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.help.BookHelp
import io.legado.app.utils.FileUtils
import io.legado.app.utils.toastOnUi

class ConfigViewModel(application: Application) : BaseViewModel(application) {

    fun clearCache() {
        execute {
            BookHelp.clearCache()
            FileUtils.delete(context.cacheDir.absolutePath)
        }.onSuccess {
            context.toastOnUi(R.string.clear_cache_success)
        }
    }


}