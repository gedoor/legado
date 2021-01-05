package io.legado.app.model.webBook

import android.text.TextUtils
import io.legado.app.App
import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.TocRule
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.Debug
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object BookChapterList {

    suspend fun analyzeChapterList(
        scope: CoroutineScope,
        book: Book,
        body: String?,
        bookSource: BookSource,
        baseUrl: String
    ): List<BookChapter> = suspendCancellableCoroutine { block ->
        kotlin.runCatching {
            val chapterList = ArrayList<BookChapter>()
            body ?: throw Exception(
                App.INSTANCE.getString(R.string.error_get_web_content, baseUrl)
            )
            Debug.log(bookSource.bookSourceUrl, "≡获取成功:${baseUrl}")

            val tocRule = bookSource.getTocRule()
            val nextUrlList = arrayListOf(baseUrl)
            var reverse = false
            var listRule = tocRule.chapterList ?: ""
            if (listRule.startsWith("-")) {
                reverse = true
                listRule = listRule.substring(1)
            }
            if (listRule.startsWith("+")) {
                listRule = listRule.substring(1)
            }
            var chapterData =
                analyzeChapterList(
                    scope, book, baseUrl, body, tocRule, listRule, bookSource, log = true
                )
            chapterData.chapterList?.let {
                chapterList.addAll(it)
            }
            when (chapterData.nextUrl.size) {
                0 -> {
                    block.resume(finish(book, chapterList, reverse))
                }
                1 -> {
                    Coroutine.async(scope = scope) {
                        var nextUrl = chapterData.nextUrl[0]
                        while (nextUrl.isNotEmpty() && !nextUrlList.contains(nextUrl)) {
                            nextUrlList.add(nextUrl)
                            AnalyzeUrl(
                                ruleUrl = nextUrl,
                                book = book,
                                headerMapF = bookSource.getHeaderMap()
                            ).getStrResponse(bookSource.bookSourceUrl).body?.let { nextBody ->
                                chapterData = analyzeChapterList(
                                    this, book, nextUrl, nextBody, tocRule, listRule, bookSource
                                )
                                nextUrl = chapterData.nextUrl.firstOrNull() ?: ""
                                chapterData.chapterList?.let {
                                    chapterList.addAll(it)
                                }
                            }
                        }
                        Debug.log(bookSource.bookSourceUrl, "◇目录总页数:${nextUrlList.size}")
                        block.resume(finish(book, chapterList, reverse))
                    }.onError {
                        block.resumeWithException(it)
                    }
                }
                else -> {
                    val chapterDataList = arrayListOf<ChapterData<String>>()
                    for (item in chapterData.nextUrl) {
                        if (!nextUrlList.contains(item)) {
                            val data = ChapterData(nextUrl = item)
                            chapterDataList.add(data)
                            nextUrlList.add(item)
                        }
                    }
                    Debug.log(bookSource.bookSourceUrl, "◇目录总页数:${nextUrlList.size}")
                    for (item in chapterDataList) {
                        downloadToc(
                            scope, item, book, bookSource,
                            tocRule, listRule, chapterList, chapterDataList,
                            {
                                block.resume(finish(book, chapterList, reverse))
                            }
                        ) {
                            block.cancel(it)
                        }
                    }
                }
            }
        }.onFailure {
            block.cancel(it)
        }
    }

    private fun downloadToc(
        scope: CoroutineScope,
        chapterData: ChapterData<String>,
        book: Book,
        bookSource: BookSource,
        tocRule: TocRule,
        listRule: String,
        chapterList: ArrayList<BookChapter>,
        chapterDataList: ArrayList<ChapterData<String>>,
        onFinish: () -> Unit,
        onError: (error: Throwable) -> Unit
    ) {
        Coroutine.async(scope = scope) {
            val nextBody = AnalyzeUrl(
                ruleUrl = chapterData.nextUrl,
                book = book,
                headerMapF = bookSource.getHeaderMap()
            ).getStrResponse(bookSource.bookSourceUrl).body
                ?: throw Exception("${chapterData.nextUrl}, 下载失败")
            val nextChapterData = analyzeChapterList(
                this, book, chapterData.nextUrl, nextBody, tocRule, listRule, bookSource, false
            )
            synchronized(chapterDataList) {
                val isFinished = addChapterListIsFinish(
                    chapterDataList,
                    chapterData,
                    nextChapterData.chapterList
                )
                if (isFinished) {
                    chapterDataList.forEach { item ->
                        item.chapterList?.let {
                            chapterList.addAll(it)
                        }
                    }
                    onFinish()
                }
            }
        }.onError {
            onError.invoke(it)
        }
    }

    private fun addChapterListIsFinish(
        chapterDataList: ArrayList<ChapterData<String>>,
        chapterData: ChapterData<String>,
        chapterList: List<BookChapter>?
    ): Boolean {
        chapterData.chapterList = chapterList
        chapterDataList.forEach {
            if (it.chapterList == null) {
                return false
            }
        }
        return true
    }

    private fun finish(
        book: Book,
        chapterList: ArrayList<BookChapter>,
        reverse: Boolean
    ): ArrayList<BookChapter> {
        //去重
        if (!reverse) {
            chapterList.reverse()
        }
        val lh = LinkedHashSet(chapterList)
        val list = ArrayList(lh)
        list.reverse()
        Debug.log(book.origin, "◇目录总数:${list.size}")
        for ((index, item) in list.withIndex()) {
            item.index = index
        }
        book.latestChapterTitle = list.last().title
        book.durChapterTitle =
            list.getOrNull(book.durChapterIndex)?.title ?: book.latestChapterTitle
        if (book.totalChapterNum < list.size) {
            book.lastCheckCount = list.size - book.totalChapterNum
            book.latestChapterTime = System.currentTimeMillis()
        }
        book.totalChapterNum = list.size
        return list
    }

    private fun analyzeChapterList(
        scope: CoroutineScope,
        book: Book,
        baseUrl: String,
        body: String,
        tocRule: TocRule,
        listRule: String,
        bookSource: BookSource,
        getNextUrl: Boolean = true,
        log: Boolean = false
    ): ChapterData<List<String>> {
        val analyzeRule = AnalyzeRule(book)
        analyzeRule.setContent(body).setBaseUrl(baseUrl)
        val chapterList = arrayListOf<BookChapter>()
        val nextUrlList = arrayListOf<String>()
        val nextTocRule = tocRule.nextTocUrl
        if (getNextUrl && !nextTocRule.isNullOrEmpty()) {
            Debug.log(bookSource.bookSourceUrl, "┌获取目录下一页列表", log)
            analyzeRule.getStringList(nextTocRule, true)?.let {
                for (item in it) {
                    if (item != baseUrl) {
                        nextUrlList.add(item)
                    }
                }
            }
            Debug.log(
                bookSource.bookSourceUrl,
                "└" + TextUtils.join("，\n", nextUrlList),
                log
            )
        }
        Debug.log(bookSource.bookSourceUrl, "┌获取目录列表", log)
        val elements = analyzeRule.getElements(listRule)
        Debug.log(bookSource.bookSourceUrl, "└列表大小:${elements.size}", log)
        scope.ensureActive()
        if (elements.isNotEmpty()) {
            val nameRule = analyzeRule.splitSourceRule(tocRule.chapterName)
            val urlRule = analyzeRule.splitSourceRule(tocRule.chapterUrl)
            val vipRule = analyzeRule.splitSourceRule(tocRule.isVip)
            val update = analyzeRule.splitSourceRule(tocRule.updateTime)
            var isVip: String?
            for (item in elements) {
                scope.ensureActive()
                analyzeRule.setContent(item)
                val bookChapter = BookChapter(bookUrl = book.bookUrl, baseUrl = baseUrl)
                analyzeRule.chapter = bookChapter
                bookChapter.title = analyzeRule.getString(nameRule)
                bookChapter.url = analyzeRule.getString(urlRule)
                bookChapter.tag = analyzeRule.getString(update)
                isVip = analyzeRule.getString(vipRule)
                if (bookChapter.url.isEmpty()) {
                    bookChapter.url = baseUrl
                }
                if (bookChapter.title.isNotEmpty()) {
                    if (isVip.isNotEmpty() && isVip != "null" && isVip != "false" && isVip != "0") {
                        bookChapter.title = "\uD83D\uDD12" + bookChapter.title
                    }
                    chapterList.add(bookChapter)
                }
            }
            Debug.log(bookSource.bookSourceUrl, "┌获取首章名称", log)
            Debug.log(bookSource.bookSourceUrl, "└${chapterList[0].title}", log)
            Debug.log(bookSource.bookSourceUrl, "┌获取首章链接", log)
            Debug.log(bookSource.bookSourceUrl, "└${chapterList[0].url}", log)
            Debug.log(bookSource.bookSourceUrl, "┌获取首章信息", log)
            Debug.log(bookSource.bookSourceUrl, "└${chapterList[0].tag}", log)
        }
        return ChapterData(chapterList, nextUrlList)
    }

}