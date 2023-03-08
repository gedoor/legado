package io.legado.app.ui.book.import.remote

import android.app.Application
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.Server

class ServerConfigViewModel(application: Application): BaseViewModel(application) {

    var server: Server? = null

    fun init(id: Long?, onSuccess: () -> Unit) {
        execute {
            if (server == null && id != null) {
                server = appDb.serverDao.get(id)
            }
        }.onSuccess {
            onSuccess.invoke()
        }
    }

}