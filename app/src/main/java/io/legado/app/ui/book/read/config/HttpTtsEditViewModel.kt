package io.legado.app.ui.book.read.config

import android.app.Application
import android.os.Bundle
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.HttpTTS
import io.legado.app.model.NoStackTraceException
import io.legado.app.model.ReadAloud
import io.legado.app.utils.*

class HttpTtsEditViewModel(app: Application) : BaseViewModel(app) {

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

    fun importFromClip(onSuccess: (httpTTS: HttpTTS) -> Unit) {
        val text = context.getClipText()
        if (text.isNullOrBlank()) {
            context.toastOnUi("剪贴板为空")
        } else {
            importSource(text, onSuccess)
        }
    }

    fun importSource(text: String, onSuccess: (httpTTS: HttpTTS) -> Unit) {
        val text1 = text.trim()
        execute {
            when {
                text1.isJsonObject() -> {
                    HttpTTS.fromJson(text1)
                }
                text1.isJsonArray() -> {
                    HttpTTS.fromJsonArray(text1).firstOrNull()
                }
                else -> {
                    throw NoStackTraceException("格式不对")
                }
            }
        }.onSuccess {
            it?.let { httpTts ->
                onSuccess.invoke(httpTts)
            } ?: context.toastOnUi("格式不对")
        }.onError {
            context.toastOnUi(it.localizedMessage)
        }
    }

}