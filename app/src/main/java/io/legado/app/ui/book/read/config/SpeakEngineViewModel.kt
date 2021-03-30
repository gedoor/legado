package io.legado.app.ui.book.read.config

import android.app.Application
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.HttpTTS
import io.legado.app.help.DefaultData
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonArray
import rxhttp.wrapper.param.RxHttp
import rxhttp.wrapper.param.toText

class SpeakEngineViewModel(application: Application) : BaseViewModel(application) {

    fun importDefault() {
        execute {
            DefaultData.importDefaultHttpTTS()
        }
    }

    fun importOnLine(url: String) {
        execute {
            RxHttp.get(url).toText("utf-8").await().let { json ->
                GSON.fromJsonArray<HttpTTS>(json)?.let {
                    appDb.httpTTSDao.insert(*it.toTypedArray())
                }
            }
        }.onSuccess {
            toastOnUi("导入成功")
        }.onError {
            toastOnUi("导入失败")
        }
    }

}