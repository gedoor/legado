package io.legado.app.ui.association

import android.app.Application
import android.os.Bundle
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb

class OpenUrlConfirmViewModel(app: Application): BaseViewModel(app) {

    var uri = ""
    var mimeType: String? = null
    var sourceOrigin = ""
    var sourceName = ""

    fun initData(arguments: Bundle) {
        uri = arguments.getString("uri") ?: ""
        mimeType = arguments.getString("mimeType")
        sourceName = arguments.getString("sourceName") ?: ""
        sourceOrigin = arguments.getString("sourceOrigin") ?: ""
    }

    fun disableSource(block: () -> Unit) {
        execute {
            appDb.bookSourceDao.enable(sourceOrigin, false)
        }.onSuccess {
            block.invoke()
        }
    }

    fun deleteSource(block: () -> Unit) {
        execute {
            appDb.bookSourceDao.delete(sourceOrigin)
        }.onSuccess {
            block.invoke()
        }
    }

}
