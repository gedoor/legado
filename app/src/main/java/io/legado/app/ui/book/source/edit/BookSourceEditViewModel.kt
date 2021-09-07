package io.legado.app.ui.book.source.edit

import android.app.Application
import android.content.Intent
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSource
import io.legado.app.help.BookSourceAnalyzer
import io.legado.app.help.http.newCallStrResponse
import io.legado.app.help.http.okHttpClient
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers

class BookSourceEditViewModel(application: Application) : BaseViewModel(application) {

    var bookSource: BookSource? = null
    private var oldSourceUrl: String? = null

    fun initData(intent: Intent, onFinally: () -> Unit) {
        execute {
            val key = intent.getStringExtra("data")
            var source: BookSource? = null
            if (key != null) {
                source = appDb.bookSourceDao.getBookSource(key)
            }
            source?.let {
                oldSourceUrl = it.bookSourceUrl
                bookSource = it
            }
        }.onFinally {
            onFinally()
        }
    }

    fun save(source: BookSource, success: (() -> Unit)? = null) {
        execute {
            oldSourceUrl?.let {
                if (oldSourceUrl != source.bookSourceUrl) {
                    appDb.bookSourceDao.delete(it)
                }
            }
            oldSourceUrl = source.bookSourceUrl
            source.lastUpdateTime = System.currentTimeMillis()
            appDb.bookSourceDao.insert(source)
            bookSource = source
        }.onSuccess {
            success?.invoke()
        }.onError {
            context.toastOnUi(it.localizedMessage)
            it.printStackTrace()
        }
    }

    fun pasteSource(onSuccess: (source: BookSource) -> Unit) {
        execute(context = Dispatchers.Main) {
            val text = context.getClipText()
            if (text.isNullOrBlank()) {
                error("剪贴板为空")
            } else {
                importSource(text, onSuccess)
            }
        }.onError {
            context.toastOnUi(it.localizedMessage ?: "Error")
            it.printStackTrace()
        }
    }

    fun importSource(text: String, finally: (source: BookSource) -> Unit) {
        execute {
            importSource(text)
        }.onSuccess {
            it?.let(finally) ?: context.toastOnUi("格式不对")
        }.onError {
            context.toastOnUi(it.localizedMessage ?: "Error")
        }
    }

    suspend fun importSource(text: String): BookSource? {
        return when {
            text.isAbsUrl() -> {
                val text1 = okHttpClient.newCallStrResponse { url(text) }.body
                text1?.let { importSource(text1) }
            }
            text.isJsonArray() -> {
                val items: List<Map<String, Any>> = jsonPath.parse(text).read("$")
                val jsonItem = jsonPath.parse(items[0])
                BookSourceAnalyzer.jsonToBookSource(jsonItem.jsonString())
            }
            text.isJsonObject() -> {
                BookSourceAnalyzer.jsonToBookSource(text)
            }
            else -> {
                null
            }
        }
    }
}