package io.legado.app.ui.book.read.config

import android.app.Application
import android.os.Bundle
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.HttpTTS
import io.legado.app.model.ReadAloud

class SpeakEngineEditViewModel(app: Application) : BaseViewModel(app) {

    var id: Long? = null

    fun initData(arguments: Bundle?, success: (httpTTS: HttpTTS) -> Unit) {
        execute {
            if (id == null) {
                id = arguments?.getLong("id")
                val httpTTS = id?.let {
                    return@let appDb.httpTTSDao.get(it)
                }
                return@execute httpTTS
            }
            return@execute null
        }.onSuccess {
            it?.let {
                success.invoke(it)
            }
        }
    }

    fun save(httpTTS: HttpTTS, success: (() -> Unit)? = null) {
        execute {
            appDb.httpTTSDao.insert(httpTTS)
            ReadAloud.upReadAloudClass()
        }.onSuccess {
            success?.invoke()
        }
    }

}