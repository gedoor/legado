package io.legado.app.ui.book.import.remote

import android.app.Application
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.Server
import io.legado.app.utils.toastOnUi

class ServerConfigViewModel(application: Application): BaseViewModel(application) {

    var mServer: Server? = null

    fun init(id: Long?, onSuccess: () -> Unit) {
        execute {
            if (mServer == null && id != null) {
                mServer = appDb.serverDao.get(id)
                mServer
            } else {
                null
            }
        }.onSuccess {
            onSuccess.invoke()
        }
    }

    fun save(server: Server, onSuccess: () -> Unit) {
        execute {
            mServer?.let {
                appDb.serverDao.delete(it)
            }
            mServer = server
            appDb.serverDao.insert(server)
        }.onSuccess {
            onSuccess.invoke()
        }.onError {
            context.toastOnUi("保存出错\n${it.localizedMessage}")
        }
    }

}