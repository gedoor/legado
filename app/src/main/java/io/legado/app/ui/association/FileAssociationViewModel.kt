package io.legado.app.ui.association

import android.app.Application
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import io.legado.app.constant.AppLog
import io.legado.app.constant.AppPattern
import io.legado.app.constant.AppPattern.bookFileRegex
import io.legado.app.model.localBook.LocalBook
import io.legado.app.utils.*

class FileAssociationViewModel(application: Application) : BaseAssociationViewModel(application) {
    val importBookLiveData = MutableLiveData<Uri>()
    val onLineImportLive = MutableLiveData<Uri>()
    val openBookLiveData = MutableLiveData<String>()
    val notSupportedLiveData = MutableLiveData<Pair<Uri, String>>()

    fun dispatchIntent(uri: Uri) {
        execute {
            //如果是普通的url，需要根据返回的内容判断是什么
            if (uri.isContentScheme() || uri.isFileScheme()) {
                val fileDoc = FileDoc.fromUri(uri, false)
                val fileName = fileDoc.name
                if (fileName.matches(AppPattern.archiveFileRegex)) {
                    ArchiveUtils.deCompress(fileDoc, ArchiveUtils.TEMP_PATH) {
                        it.matches(bookFileRegex)
                    }.forEach {
                        dispatch(FileDoc.fromFile(it))
                    }
                } else {
                    dispatch(fileDoc)
                }
            } else {
                onLineImportLive.postValue(uri)
            }
        }.onError {
            it.printOnDebug()
            val msg = "无法打开文件\n${it.localizedMessage}"
            errorLive.postValue(msg)
            AppLog.put(msg, it)
        }
    }

    private fun dispatch(fileDoc: FileDoc) {
        kotlin.runCatching {
            if (fileDoc.openInputStream().getOrNull().isJson()) {
                importJson(fileDoc.uri)
                return
            }
        }.onFailure {
            it.printOnDebug()
            AppLog.put("尝试导入为JSON文件失败\n${it.localizedMessage}", it)
        }
        if (fileDoc.name.matches(bookFileRegex)) {
            importBookLiveData.postValue(fileDoc.uri)
            return
        }
        notSupportedLiveData.postValue(Pair(fileDoc.uri, fileDoc.name))
    }

    fun importBook(uri: Uri) {
        val book = LocalBook.importFile(uri)
        openBookLiveData.postValue(book.bookUrl)
    }
}