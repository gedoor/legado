package io.legado.app.model.webBook

import android.text.TextUtils
import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.TocRule
import io.legado.app.model.Debug
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

import splitties.init.appCtx

object BookChapterList {

    suspend fun analyzeChapterList(
        scope: CoroutineScope,
        book: Book,
        body: String?,
        bookSource: BookSource,
        baseUrl: String,
        redirectUrl: String
    ): List<BookChapter> {
        val chapterList = ArrayList<BookChapter>()
        body ?: throw Exception(
            appCtx.getString(R.string.error_get_web_content, baseUrl)
        )
        Debug.log(bookSource.bookSourceUrl, "≡获取成功:${baseUrl}")
        Debug.log(bookSource.bookSourceUrl, body, state = 30)
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
                scope, book, baseUrl, redirectUrl, body,
                tocRule, listRule, bookSource, log = true
            )
        chapterData.chapterList?.let {
            chapterList.addAll(it)
        }
        when (chapterData.nextUrl.size) {
            0 -> Unit
            1 -> {
                var nextUrl = chapterData.nextUrl[0]
                while (nextUrl.isNotEmpty() && !nextUrlList.contains(nextUrl)) {
                    nextUrlList.add(nextUrl)
                    AnalyzeUrl(
                        ruleUrl = nextUrl,
                        book = book,
                        headerMapF = bookSource.getHeaderMap()
                    ).getStrResponse(bookSource.bookSourceUrl).body?.let { nextBody ->
                        chapterData = analyzeChapterList(
                            scope, book, nextUrl, nextUrl,
                            nextBody, tocRule, listRule, bookSource
                        )
                        nextUrl = chapterData.nextUrl.firstOrNull() ?: ""
                        chapterData.chapterList?.let {
                            chapterList.addAll(it)
                        }
                    }
                }
                Debug.log(bookSource.bookSourceUrl, "◇目录总页数:${nextUrlList.size}")
            }
            else -> {
                Debug.log(bookSource.bookSourceUrl, "◇并发解析目录,总页数:${chapterData.nextUrl.size}")
                withContext(IO) {
                    val asyncArray = Array(chapterData.nextUrl.size) {
                        async(IO) {
                            val urlStr = chapterData.nextUrl[it]
                            val analyzeUrl = AnalyzeUrl(
                                ruleUrl = urlStr,
                                book = book,
                                headerMapF = bookSource.getHeaderMap()
                            )
                            val res = analyzeUrl.getStrResponse(bookSource.bookSourceUrl)
                            analyzeChapterList(
                                this, book, urlStr, res.url,
                                res.body!!, tocRule, listRule, bookSource, false
                            ).chapterList
                        }
                    }
                    asyncArray.forEach { coroutine ->
                        coroutine.await()?.let {
                            chapterList.addAll(it)
                        }
                    }
                }
            }
        }
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
        redirectUrl: String,
        body: String,
        tocRule: TocRule,
        listRule: String,
        bookSource: BookSource,
        getNextUrl: Boolean = true,
        log: Boolean = false
    ): ChapterData<List<String>> {
        val analyzeRule = AnalyzeRule(book)
        analyzeRule.setContent(body).setBaseUrl(baseUrl)
        analyzeRule.setRedirectUrl(redirectUrl)
        //获取目录列表
        val chapterList = arrayListOf<BookChapter>()
        Debug.log(bookSource.bookSourceUrl, "┌获取目录列表", log)
        val elements = analyzeRule.getElements(listRule)
        Debug.log(bookSource.bookSourceUrl, "└列表大小:${elements.size}", log)
        //获取下一页链接
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
        scope.ensureActive()
        if (elements.isNotEmpty()) {
            Debug.log(bookSource.bookSourceUrl, "┌解析目录列表", log)
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
            Debug.log(bookSource.bookSourceUrl, "└解析目录列表完成", log)
            Debug.log(bookSource.bookSourceUrl, "┌首章名称", log)
            Debug.log(bookSource.bookSourceUrl, "└${chapterList[0].title}", log)
            Debug.log(bookSource.bookSourceUrl, "┌首章链接", log)
            Debug.log(bookSource.bookSourceUrl, "└${chapterList[0].url}", log)
            Debug.log(bookSource.bookSourceUrl, "┌首章信息", log)
            Debug.log(bookSource.bookSourceUrl, "└${chapterList[0].tag}", log)
        }
        return ChapterData(chapterList, nextUrlList)
    }

}