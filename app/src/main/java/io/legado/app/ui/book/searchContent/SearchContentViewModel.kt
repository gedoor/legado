package io.legado.app.ui.book.searchContent


import android.app.Application
import com.github.liuyueyi.quick.transfer.ChineseUtils
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.BookHelp
import io.legado.app.help.ContentProcessor
import io.legado.app.help.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class SearchContentViewModel(application: Application) : BaseViewModel(application) {
    var bookUrl: String = ""
    var book: Book? = null
    private var contentProcessor: ContentProcessor? = null
    var lastQuery: String = ""
    var searchResultCounts = 0
    val cacheChapterNames = hashSetOf<String>()
    val searchResultList: MutableList<SearchResult> = mutableListOf()
    var mContent: String = ""

    fun initBook(bookUrl: String, success: () -> Unit) {
        this.bookUrl = bookUrl
        execute {
            book = appDb.bookDao.getBook(bookUrl)
            book?.let {
                contentProcessor = ContentProcessor.get(it.name, it.origin)
            }
        }.onSuccess {
            success.invoke()
        }
    }

    suspend fun searchChapter(
        query: String,
        chapter: BookChapter?
    ): List<SearchResult> {
        val searchResultsWithinChapter: MutableList<SearchResult> = mutableListOf()
        if (chapter != null) {
            book?.let { book ->
                val chapterContent = BookHelp.getContent(book, chapter)
                coroutineContext.ensureActive()
                if (chapterContent != null) {
                    //先搜索没有启用净化的正文
                    withContext(Dispatchers.IO) {
                        chapter.title = when (AppConfig.chineseConverterType) {
                            1 -> ChineseUtils.t2s(chapter.title)
                            2 -> ChineseUtils.s2t(chapter.title)
                            else -> chapter.title
                        }
                        kotlin.coroutines.coroutineContext.ensureActive()
                        mContent = contentProcessor!!.getContent(
                            book, chapter, chapterContent,
                            chineseConvert = true,
                            reSegment = false,
                            useReplace = false
                        ).joinToString("")
                    }
                    val positions = searchPosition(query)
                    positions.forEachIndexed { index, position ->
                        coroutineContext.ensureActive()
                        val construct = getResultAndQueryIndex(mContent, position, query)
                        val result = SearchResult(
                            resultCountWithinChapter = index,
                            resultText = construct.second,
                            chapterTitle = chapter.title,
                            query = query,
                            chapterIndex = chapter.index,
                            queryIndexInResult = construct.first,
                            queryIndexInChapter = position
                        )
                        searchResultsWithinChapter.add(result)
                    }
                    searchResultCounts += searchResultsWithinChapter.size
                }
            }
        }
        return searchResultsWithinChapter
    }

    private suspend fun searchPosition(pattern: String): List<Int> {
        val position: MutableList<Int> = mutableListOf()
        var index = mContent.indexOf(pattern)
        if (index >= 0) {
            //搜索到内容允许净化
            if (book!!.getUseReplaceRule()) {
                mContent = contentProcessor!!.replaceContent(mContent)
                index = mContent.indexOf(pattern)
            }
            while (index >= 0) {
                coroutineContext.ensureActive()
                position.add(index)
                index = mContent.indexOf(pattern, index + pattern.length)
            }
        }
        return position
    }

    private fun getResultAndQueryIndex(
        content: String,
        queryIndexInContent: Int,
        query: String
    ): Pair<Int, String> {
        // 左右移动20个字符，构建关键词周边文字，在搜索结果里显示
        // todo: 判断段落，只在关键词所在段落内分割
        // todo: 利用标点符号分割完整的句
        // todo: length和设置结合，自由调整周边文字长度
        val length = 20
        var po1 = queryIndexInContent - length
        var po2 = queryIndexInContent + query.length + length
        if (po1 < 0) {
            po1 = 0
        }
        if (po2 > content.length) {
            po2 = content.length
        }
        val queryIndexInResult = queryIndexInContent - po1
        val newText = content.substring(po1, po2)
        return queryIndexInResult to newText
    }

}