package io.legado.app.ui.association

import android.app.Application
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppPattern
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.exception.NoStackTraceException
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.model.localBook.LocalBook
import io.legado.app.utils.*

class ImportOnLineBookFileViewModel(app: Application) : BaseViewModel(app) {

    val allBookFiles = arrayListOf<Triple<String, String, Boolean>>()
    val selectStatus = arrayListOf<Boolean>()
    val errorLiveData = MutableLiveData<String>()
    val successLiveData = MutableLiveData<Int>()

    fun initData(bookUrl: String?, infoHtml: String?) {
        execute {
            bookUrl ?: throw NoStackTraceException("书籍详情页链接为空")
            val book = appDb.searchBookDao.getSearchBook(bookUrl)?.toBook()
                ?: throw NoStackTraceException("book is null")
            val bookSource = appDb.bookSourceDao.getBookSource(book.origin)
                ?: throw NoStackTraceException("bookSource is null")
            val ruleDownloadUrls = bookSource?.getBookInfoRule()?.downloadUrls
            var content = infoHtml
            if (content.isNullOrBlank()) {
                content = AnalyzeUrl(bookUrl, source = bookSource).getStrResponse().body
            }
            val analyzeRule = AnalyzeRule(book, bookSource)
            analyzeRule.setContent(content).setBaseUrl(bookUrl)
            analyzeRule.getStringList(ruleDownloadUrls, isUrl = true)?.let {
                it.forEach { url ->
                    val fileName = LocalBook.extractDownloadName(url, book)
                    val isSupportedFile = AppPattern.bookFileRegex.matches(fileName)
                    allBookFiles.add(Triple(url, fileName, isSupportedFile))
                    selectStatus.add(isSupportedFile)
                }
            } ?: throw NoStackTraceException("下载链接规则解析为空")
        }.onSuccess {
            successLiveData.postValue(allBookFiles.size)
        }.onError {
            errorLiveData.postValue(it.localizedMessage ?: "")
            context.toastOnUi("获取书籍下载链接失败\n${it.localizedMessage}")
        }
        
    }

    val isSelectAll: Boolean
        get() {
            selectStatus.forEach {
                if (!it) {
                    return false
                }
            }
            return true
        }

    val selectCount: Int
        get() {
            var count = 0
            selectStatus.forEach {
                if (it) {
                    count++
                }
            }
            return count
        }

    fun importSelect(success: () -> Unit) {
        execute {
            selectStatus.forEachIndexed { index, selected ->
                if (selected) {
                    val selectedFile = allBookFiles[index]
                    val isSupportedFile = selectedFile.third
                    val fileUrl: String = selectedFile.first
                    val fileName: String = selectedFile.second
                    when {
                        isSupportedFile -> importOnLineBookFile(fileUrl, fileName)
                        else -> downloadUrl(fileUrl, fileName)
                    }
                }
            }
        }.onSuccess {
            success.invoke()
        }.onError {
        
        }
    }


    fun downloadUrl(url: String, fileName: String): Uri {
        return LocalBook.saveBookFile(url, fileName)
    }

    fun importOnLineBookFile(url: String, fileName: String) {
        LocalBook.importFileOnLine(url, fileName)
    }

}
