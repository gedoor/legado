package io.legado.app.ui.main

import android.app.Application
import io.legado.app.base.BaseViewModel
import io.legado.app.help.storage.Restore
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : BaseViewModel(application) {

    fun restore() {
        launch(IO) {
            Restore.importYueDuData(getApplication())
        }
    }
}