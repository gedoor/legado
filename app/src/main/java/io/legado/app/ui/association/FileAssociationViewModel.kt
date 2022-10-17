package io.legado.app.ui.association

import android.app.Application
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import io.legado.app.constant.AppLog
import io.legado.app.constant.AppPattern.bookFileRegex
import io.legado.app.model.localBook.LocalBook
import io.legado.app.utils.*

class FileAssociationViewModel(application: Application) : BaseAssociationViewModel(application) {
    val importBookLiveData = MutableLiveData<Uri>()
    val onLineImportLive = MutableLiveData<Uri>()
    val openBookLiveData = MutableLiveData<String>()
    val notSupportedLiveData = MutableLiveData<Pair<Uri, String>>()

    @Suppress("BlockingMethodInNonBlockingContext")
    fun dispatchIndent(uri: Uri) {
        execute {
            lateinit var fileName: String
            //如果是普通的url，需要根据返回的内容判断是什么
            if (uri.isContentScheme() || uri.isFileScheme()) {
                val fileDoc = FileDoc.fromUri(uri, false)
                fileName = fileDoc.name
                kotlin.runCatching {
                    if (uri.inputStream(context).getOrNull().isJson()) {
                        importJson(uri)
                        return@execute
                    }
                }.onFailure {
                    it.printOnDebug()
                    AppLog.put("尝试导入为JSON文件失败\n${it.localizedMessage}", it)
                }
                if (fileName.matches(bookFileRegex)) {
                    importBookLiveData.postValue(uri)
                    return@execute
                }
                notSupportedLiveData.postValue(Pair(uri, fileName))
            } else {
                onLineImportLive.postValue(uri)
            }
        }.onError {
            it.printOnDebug()
            errorLive.postValue(it.localizedMessage)
            AppLog.put("无法打开文件\n${it.localizedMessage}", it)
        }
    }

    fun importBook(uri: Uri) {
        val book = LocalBook.importFile(uri)
        openBookLiveData.postValue(book.bookUrl)
    }
}