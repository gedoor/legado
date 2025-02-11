package io.legado.app.ui.association

import android.app.Application
import android.os.Bundle
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb

class VerificationCodeViewModel(app: Application): BaseViewModel(app) {

    var sourceOrigin = ""
    var sourceName = ""

    fun initData(arguments: Bundle) {
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
