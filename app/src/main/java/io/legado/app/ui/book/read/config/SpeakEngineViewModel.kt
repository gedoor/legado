package io.legado.app.ui.book.read.config

import android.app.Application
import android.net.Uri
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.HttpTTS
import io.legado.app.help.DefaultData
import io.legado.app.help.http.newCall
import io.legado.app.help.http.okHttpClient
import io.legado.app.help.http.text
import io.legado.app.utils.*

class SpeakEngineViewModel(application: Application) : BaseViewModel(application) {

    fun importDefault() {
        execute {
            DefaultData.importDefaultHttpTTS()
        }
    }

    fun importOnLine(url: String) {
        execute {
            okHttpClient.newCall {
                url(url)
            }.text("utf-8").let { json ->
                import(json)
            }
        }.onSuccess {
            toastOnUi("导入成功")
        }.onError {
            toastOnUi("导入失败")
        }
    }

    fun importLocal(uri: Uri) {
        execute {
            uri.readText(context)?.let {
                import(it)
            }
        }.onSuccess {
            toastOnUi("导入成功")
        }.onError {
            toastOnUi("导入失败")
        }
    }

    fun import(text: String) {
        when {
            text.isJsonArray() -> {
                GSON.fromJsonArray<HttpTTS>(text)?.let {
                    appDb.httpTTSDao.insert(*it.toTypedArray())
                }
            }
            text.isJsonObject() -> {
                GSON.fromJsonObject<HttpTTS>(text)?.let {
                    appDb.httpTTSDao.insert(it)
                }
            }
            else -> {
                throw Exception("格式不对")
            }
        }
    }

    fun export(uri: Uri) {
        execute {
            val httpTTS = appDb.httpTTSDao.all
            uri.writeBytes(context, "httpTts.json", GSON.toJson(httpTTS).toByteArray())
        }
    }
}