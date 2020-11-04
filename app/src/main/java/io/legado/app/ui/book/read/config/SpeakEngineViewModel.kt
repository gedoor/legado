package io.legado.app.ui.book.read.config

import android.app.Application
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.HttpTTS
import io.legado.app.help.DefaultData
import io.legado.app.help.http.HttpHelper
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonArray

class SpeakEngineViewModel(application: Application) : BaseViewModel(application) {

    fun importDefault() {
        execute {
            DefaultData.httpTTS.let {
                App.db.httpTTSDao().insert(*it.toTypedArray())
            }
        }
    }

    fun importOnLine(url: String, finally: (msg: String) -> Unit) {
        execute {
            HttpHelper.simpleGetAsync(url)?.let { json ->
                GSON.fromJsonArray<HttpTTS>(json)?.let {
                    App.db.httpTTSDao().insert(*it.toTypedArray())
                }
            }
        }.onSuccess {
            finally("导入成功")
        }.onError {
            finally("导入失败")
        }
    }

}