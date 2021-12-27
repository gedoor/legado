package io.legado.app.ui.association

import android.app.Application
import android.app.RecoverableSecurityException
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.MutableLiveData
import io.legado.app.help.BookMediaStore
import io.legado.app.model.NoStackTraceException
import io.legado.app.model.localBook.LocalBook
import io.legado.app.utils.isContentScheme
import io.legado.app.utils.isJson
import io.legado.app.utils.readText
import splitties.init.appCtx
import timber.log.Timber
import java.io.File

class FileAssociationViewModel(application: Application) : BaseAssociationViewModel(application) {
    val onLineImportLive = MutableLiveData<Uri>()
    val importBookSourceLive = MutableLiveData<String>()
    val importRssSourceLive = MutableLiveData<String>()
    val importReplaceRuleLive = MutableLiveData<String>()
    val openBookLiveData = MutableLiveData<String>()
    val errorLiveData = MutableLiveData<String>()
    val recoverErrorLiveData = MutableLiveData<IntentSender>()

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
                    if (uri.isContentScheme()) {
                        val doc = DocumentFile.fromSingleUri(appCtx, uri)!!
                        val bookDoc = BookMediaStore.getBook(doc.name!!)
                        if (bookDoc == null) {
                            val bookUri = BookMediaStore.insertBook(doc)
                            val book = LocalBook.importFile(bookUri!!)
                            openBookLiveData.postValue(book.bookUrl)
                        } else {
                            if (doc.lastModified() > bookDoc.date.time) {
                                context.contentResolver.openOutputStream(bookDoc.uri)
                                    .use { outputStream ->
                                        val brr = ByteArray(1024)
                                        var len: Int
                                        val bufferedInputStream =
                                            appCtx.contentResolver.openInputStream(doc.uri)!!
                                        while ((bufferedInputStream.read(brr, 0, brr.size)
                                                .also { len = it }) != -1
                                        ) {
                                            outputStream?.write(brr, 0, len)
                                        }
                                        outputStream?.flush()
                                        bufferedInputStream.close()
                                    }
                            }
                            val book = LocalBook.importFile(bookDoc.uri)
                            openBookLiveData.postValue(book.bookUrl)
                        }
                    } else {
                        val book = LocalBook.importFile(uri)
                        openBookLiveData.postValue(book.bookUrl)
                    }
                }
            } else {
                onLineImportLive.postValue(uri)
            }
        }.onError {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (it is RecoverableSecurityException) {
                    val intentSender = it.userAction.actionIntent.intentSender
                    recoverErrorLiveData.postValue(intentSender)
                    return@onError
                }
            }
            Timber.e(it)
            errorLiveData.postValue(it.localizedMessage)
        }
    }
}