package io.legado.app.ui.association

import android.app.Application
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppPattern
import io.legado.app.constant.EventBus
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSource
import io.legado.app.exception.NoStackTraceException
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.model.localBook.LocalBook
import io.legado.app.utils.postEvent
import io.legado.app.utils.toastOnUi

class ImportOnLineBookFileViewModel(app: Application) : BaseViewModel(app) {

    val allBookFiles = arrayListOf<Triple<String, String, Boolean>>()
    val errorLiveData = MutableLiveData<String>()
    val successLiveData = MutableLiveData<Int>()
    val savedFileUriData = MutableLiveData<Uri>()
    var bookSource: BookSource? = null

    fun initData(bookUrl: String?) {
        execute {
            bookUrl ?: throw NoStackTraceException("书籍详情页链接为空")
            val book = appDb.searchBookDao.getSearchBook(bookUrl)?.toBook()
                ?: throw NoStackTraceException("book is null")
            bookSource = appDb.bookSourceDao.getBookSource(book.origin)
                ?: throw NoStackTraceException("bookSource is null")
            val ruleDownloadUrls = bookSource?.getBookInfoRule()?.downloadUrls
            val content = AnalyzeUrl(bookUrl, source = bookSource).getStrResponse().body
            val analyzeRule = AnalyzeRule(book, bookSource)
            analyzeRule.setContent(content).setBaseUrl(bookUrl)
            val fileName = "${book.name} 作者：${book.author}"
            analyzeRule.getStringList(ruleDownloadUrls, isUrl = true)?.let {
                it.forEach { url ->
                    val mFileName = "${fileName}.${LocalBook.parseFileSuffix(url)}"
                    val isSupportedFile = AppPattern.bookFileRegex.matches(mFileName)
                    allBookFiles.add(Triple(url, mFileName, isSupportedFile))
                }
            } ?: throw NoStackTraceException("下载链接规则解析为空")
        }.onSuccess {
            successLiveData.postValue(allBookFiles.size)
        }.onError {
            errorLiveData.postValue(it.localizedMessage ?: "")
            context.toastOnUi("获取书籍下载链接失败\n${it.localizedMessage}")
        }
        
    }

    fun downloadUrl(url: String, fileName: String, success: () -> Unit) {
        execute {
            LocalBook.saveBookFile(url, fileName, bookSource).let {
                savedFileUriData.postValue(it)
            }
        }.onSuccess {
            success.invoke()
        }.onError {
            context.toastOnUi("下载书籍文件失败\n${it.localizedMessage}")
        }
    }

    fun importOnLineBookFile(url: String, fileName: String, success: () -> Unit) {
        execute {
            LocalBook.importFileOnLine(url, fileName, bookSource).let {
                postEvent(EventBus.BOOK_URL_CHANGED, it.bookUrl)
            }
        }.onSuccess {
           success.invoke()
        }.onError {
            context.toastOnUi("下载书籍文件失败\n${it.localizedMessage}")
        }
    }

}
