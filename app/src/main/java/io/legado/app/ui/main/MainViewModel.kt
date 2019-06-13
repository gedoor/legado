package io.legado.app.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.legado.app.help.storage.Restore
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    fun restore() {
        GlobalScope.launch {
            Restore.importYueDuData(getApplication())
        }
    }
}