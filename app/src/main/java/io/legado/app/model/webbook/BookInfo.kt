package io.legado.app.model.webbook

import io.legado.app.App
import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeUrl

object BookInfo {

    @Throws(Exception::class)
    fun analyzeBookInfo(
        book: Book,
        body: String?,
        bookSource: BookSource,
        analyzeUrl: AnalyzeUrl
    ) {
        val baseUrl = analyzeUrl.url
        body ?: throw Exception(
            App.INSTANCE.getString(
                R.string.error_get_web_content,
                baseUrl
            )
        )
        SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取成功:$baseUrl")
        val infoRule = bookSource.getBookInfoRule()
        val analyzeRule = AnalyzeRule(book)
        analyzeRule.setContent(body, baseUrl)
        infoRule.init?.let {
            if (it.isNotEmpty()) {
                SourceDebug.printLog(bookSource.bookSourceUrl, 1, "执行详情页初始化规则")
                analyzeRule.setContent(analyzeRule.getElement(it))
            }
        }
        SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取书名")
        analyzeRule.getString(infoRule.name ?: "")?.let {
            if (it.isNotEmpty()) book.name = it
        }
        SourceDebug.printLog(bookSource.bookSourceUrl, 1, book.name)
        SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取作者")
        analyzeRule.getString(infoRule.author ?: "")?.let {
            if (it.isNotEmpty()) book.author = it
        }
        SourceDebug.printLog(bookSource.bookSourceUrl, 1, book.author)
        SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取分类")
        analyzeRule.getString(infoRule.kind ?: "")?.let {
            if (it.isNotEmpty()) book.kind = it
        }
        SourceDebug.printLog(bookSource.bookSourceUrl, 1, book.kind ?: "")
        SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取简介")
        analyzeRule.getString(infoRule.intro ?: "")?.let {
            if (it.isNotEmpty()) book.intro = it
        }
        SourceDebug.printLog(bookSource.bookSourceUrl, 1, book.intro ?: "", isHtml = true)
        SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取字数")
        analyzeRule.getString(infoRule.wordCount ?: "")?.let {
            if (it.isNotEmpty()) book.wordCount = it
        }
        SourceDebug.printLog(bookSource.bookSourceUrl, 1, book.wordCount ?: "")
        SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取最新章节")
        analyzeRule.getString(infoRule.lastChapter ?: "")?.let {
            if (it.isNotEmpty()) book.latestChapterTitle = it
        }
        SourceDebug.printLog(bookSource.bookSourceUrl, 1, book.latestChapterTitle ?: "")
        SourceDebug.printLog(bookSource.bookSourceUrl, 1, "获取目录Url")
        book.tocUrl = analyzeRule.getString(infoRule.tocUrl ?: "", true) ?: baseUrl
        if (book.tocUrl.isEmpty()) book.tocUrl = baseUrl
        SourceDebug.printLog(bookSource.bookSourceUrl, 1, book.tocUrl)
    }

}