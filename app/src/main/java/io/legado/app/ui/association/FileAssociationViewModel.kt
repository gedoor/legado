package io.legado.app.ui.association

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.MutableLiveData
import io.legado.app.model.NoStackTraceException
import io.legado.app.model.localBook.LocalBook
import io.legado.app.utils.isJson
import io.legado.app.utils.readText
import timber.log.Timber
import java.io.File

class FileAssociationViewModel(application: Application) : BaseAssociationViewModel(application) {
    val importBookLiveData = MutableLiveData<Uri>()
    val onLineImportLive = MutableLiveData<Uri>()
    val importBookSourceLive = MutableLiveData<String>()
    val importRssSourceLive = MutableLiveData<String>()
    val importReplaceRuleLive = MutableLiveData<String>()
    val openBookLiveData = MutableLiveData<String>()
    val errorLiveData = MutableLiveData<String>()

    @Suppress("BlockingMethodInNonBlockingContext")
    fun dispatchIndent(uri: Uri, finally: (title: String, msg: String) -> Unit) {
        execute {
            //如果是普通的url，需要根据返回的内容判断是什么
            if (uri.scheme == "file" || uri.scheme == "content") {
                val content = if (uri.scheme == "file") {
                    File(uri.path.toString()).readText()
                } else {
                    DocumentFile.fromSingleUri(context, uri)?.readText(context)
                } ?: throw NoStackTraceException("文件不存在")
                if (content.isJson()) {
                    //暂时根据文件内容判断属于什么
                    when {
                        content.contains("bookSourceUrl") ->
                            importBookSourceLive.postValue(content)
                        content.contains("sourceUrl") ->
                            importRssSourceLive.postValue(content)
                        content.contains("pattern") ->
                            importReplaceRuleLive.postValue(content)
                        content.contains("themeName") ->
                            importTheme(content, finally)
                        content.contains("name") && content.contains("rule") ->
                            importTextTocRule(content, finally)
                        content.contains("name") && content.contains("url") ->
                            importHttpTTS(content, finally)
                        else -> errorLiveData.postValue("格式不对")
                    }
                } else {
                    importBookLiveData.postValue(uri)
                }
            } else {
                onLineImportLive.postValue(uri)
            }
        }.onError {
            Timber.e(it)
            errorLiveData.postValue(it.localizedMessage)
        }
    }

    fun importBook(uri: Uri) {
        val book = LocalBook.importFile(uri)
        openBookLiveData.postValue(book.bookUrl)
    }
}