package io.legado.app.ui.book.read.config

import android.app.Application
import io.legado.app.App
import io.legado.app.base.BaseViewModel
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

    fun importOnLine(url: String, finally: (msg: String) -> Unit) {
        execute {
            RxHttp.get(url).toText("utf-8").await().let { json ->
                GSON.fromJsonArray<HttpTTS>(json)?.let {
                    App.db.httpTTSDao.insert(*it.toTypedArray())
                }
            }
        }.onSuccess {
            finally("导入成功")
        }.onError {
            finally("导入失败")
        }
    }

}