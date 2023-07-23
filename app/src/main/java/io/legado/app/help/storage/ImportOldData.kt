package io.legado.app.help.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.jayway.jsonpath.DocumentContext
import io.legado.app.R
import io.legado.app.constant.AppConst
import io.legado.app.constant.BookSourceType
import io.legado.app.constant.BookType
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.*
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.ReplaceAnalyzer
import io.legado.app.utils.*
import splitties.init.appCtx
import java.io.File
import java.util.regex.Pattern

object ImportOldData {

    @Suppress("RegExpRedundantEscape")
    private val headerPattern = Pattern.compile("@Header:\\{.+?\\}", Pattern.CASE_INSENSITIVE)
    @Suppress("RegExpRedundantEscape")
    private val jsPattern = Pattern.compile("\\{\\{.+?\\}\\}", Pattern.CASE_INSENSITIVE)

    fun importUri(context: Context, uri: Uri) {
        if (uri.isContentScheme()) {
            DocumentFile.fromTreeUri(context, uri)?.listFiles()?.forEach { doc ->
                when (doc.name) {
                    "myBookShelf.json" ->
                        kotlin.runCatching {
                            doc.uri.readText(context).let { json ->
                                val importCount = importOldBookshelf(json)
                                context.toastOnUi("成功导入书架${importCount}")
                            }
                        }.onFailure {
                            context.toastOnUi("导入书架失败\n${it.localizedMessage}")
                        }
                    "myBookSource.json" ->
                        kotlin.runCatching {
                            doc.uri.readText(context).let { json ->
                                val importCount = importOldSource(json)
                                context.toastOnUi("成功导入书源${importCount}")
                            }
                        }.onFailure {
                            context.toastOnUi("导入源失败\n${it.localizedMessage}")
                        }
                    "myBookReplaceRule.json" ->
                        kotlin.runCatching {
                            doc.uri.readText(context).let { json ->
                                val importCount = importOldReplaceRule(json)
                                context.toastOnUi("成功导入替换规则${importCount}")
                            }
                        }.onFailure {
                            context.toastOnUi("导入替换规则失败\n${it.localizedMessage}")
                        }
                }
            }
        } else {
            uri.path?.let { path ->
                val file = File(path)
                kotlin.runCatching {// 导入书架
                    val shelfFile =
                        FileUtils.createFileIfNotExist(file, "myBookShelf.json")
                    val json = shelfFile.readText()
                    val importCount = importOldBookshelf(json)
                    context.toastOnUi("成功导入书架${importCount}")
                }.onFailure {
                    context.toastOnUi("导入书架失败\n${it.localizedMessage}")
                }

                kotlin.runCatching {// Book source
                    val sourceFile =
                        file.getFile("myBookSource.json")
                    val json = sourceFile.readText()
                    val importCount = importOldSource(json)
                    context.toastOnUi("成功导入书源${importCount}")
                }.onFailure {
                    context.toastOnUi("导入源失败\n${it.localizedMessage}")
                }

                kotlin.runCatching {// Replace rules
                    val ruleFile = file.getFile("myBookReplaceRule.json")
                    if (ruleFile.exists()) {
                        val json = ruleFile.readText()
                        val importCount = importOldReplaceRule(json)
                        context.toastOnUi("成功导入替换规则${importCount}")
                    } else {
                        context.toastOnUi("未找到替换规则")
                    }
                }.onFailure {
                    context.toastOnUi("导入替换规则失败\n${it.localizedMessage}")
                }
            }
        }
    }

    private fun importOldBookshelf(json: String): Int {
        val books = fromOldBooks(json)
        appDb.bookDao.insert(*books.toTypedArray())
        return books.size
    }

    fun importOldSource(json: String): Int {
        val sources = fromOldBookSources(json)
        appDb.bookSourceDao.insert(*sources.toTypedArray())
        return sources.size
    }

    private fun importOldReplaceRule(json: String): Int {
        val rules = ReplaceAnalyzer.jsonToReplaceRules(json).getOrNull()
        rules?.let {
            appDb.replaceRuleDao.insert(*rules.toTypedArray())
            return rules.size
        }
        return 0
    }

    private fun fromOldBooks(json: String): List<Book> {
        val books = mutableListOf<Book>()
        val items: List<Map<String, Any>> = jsonPath.parse(json).read("$")
        val existingBooks = appDb.bookDao.allBookUrls.toSet()
        for (item in items) {
            val jsonItem = jsonPath.parse(item)
            val book = Book()
            book.bookUrl = jsonItem.readString("$.noteUrl") ?: ""
            if (book.bookUrl.isBlank()) continue
            book.name = jsonItem.readString("$.bookInfoBean.name") ?: ""
            if (book.bookUrl in existingBooks) {
                DebugLog.d(javaClass.name, "Found existing book: " + book.name)
                continue
            }
            book.origin = jsonItem.readString("$.tag") ?: ""
            book.originName = jsonItem.readString("$.bookInfoBean.origin") ?: ""
            book.author = jsonItem.readString("$.bookInfoBean.author") ?: ""
            val local = if (book.origin == "loc_book") BookType.local else 0
            val isAudio = jsonItem.readString("$.bookInfoBean.bookSourceType") == "AUDIO"
            book.type = local or if (isAudio) BookType.audio else BookType.text
            book.tocUrl = jsonItem.readString("$.bookInfoBean.chapterUrl") ?: book.bookUrl
            book.coverUrl = jsonItem.readString("$.bookInfoBean.coverUrl")
            book.customCoverUrl = jsonItem.readString("$.customCoverPath")
            book.lastCheckTime = jsonItem.readLong("$.bookInfoBean.finalRefreshData") ?: 0
            book.canUpdate = jsonItem.readBool("$.allowUpdate") == true
            book.totalChapterNum = jsonItem.readInt("$.chapterListSize") ?: 0
            book.durChapterIndex = jsonItem.readInt("$.durChapter") ?: 0
            book.durChapterTitle = jsonItem.readString("$.durChapterName")
            book.durChapterPos = jsonItem.readInt("$.durChapterPage") ?: 0
            book.durChapterTime = jsonItem.readLong("$.finalDate") ?: 0
            book.intro = jsonItem.readString("$.bookInfoBean.introduce")
            book.latestChapterTitle = jsonItem.readString("$.lastChapterName")
            book.lastCheckCount = jsonItem.readInt("$.newChapters") ?: 0
            book.order = jsonItem.readInt("$.serialNumber") ?: 0
            book.variable = jsonItem.readString("$.variable")
            book.setUseReplaceRule(jsonItem.readBool("$.useReplaceRule") == true)
            books.add(book)
        }
        return books
    }

    private fun fromOldBookSources(json: String): MutableList<BookSource> {
        val sources = mutableListOf<BookSource>()
        val items: List<Map<String, Any>> = jsonPath.parse(json).read("$")
        for (item in items) {
            val jsonItem = jsonPath.parse(item)
            val source = fromOldBookSource(jsonItem)
            sources.add(source)
        }
        return sources
    }

    fun fromOldBookSource(jsonItem: DocumentContext): BookSource {
        val source = BookSource()
        return source.apply {
            bookSourceUrl = jsonItem.readString("bookSourceUrl")
                ?: throw NoStackTraceException(appCtx.getString(R.string.wrong_format))
            bookSourceName = jsonItem.readString("bookSourceName") ?: ""
            bookSourceGroup = jsonItem.readString("bookSourceGroup")
            loginUrl = jsonItem.readString("loginUrl")
            loginUi = jsonItem.readString("loginUi")
            loginCheckJs = jsonItem.readString("loginCheckJs")
            coverDecodeJs = jsonItem.readString("coverDecodeJs")
            bookSourceComment = jsonItem.readString("bookSourceComment") ?: ""
            bookUrlPattern = jsonItem.readString("ruleBookUrlPattern")
            customOrder = jsonItem.readInt("serialNumber") ?: 0
            header = uaToHeader(jsonItem.readString("httpUserAgent"))
            searchUrl = toNewUrl(jsonItem.readString("ruleSearchUrl"))
            exploreUrl = toNewUrls(jsonItem.readString("ruleFindUrl"))
            bookSourceType =
                if (jsonItem.readString("bookSourceType") == "AUDIO") BookSourceType.audio else BookSourceType.default
            enabled = jsonItem.readBool("enable") ?: true
            if (exploreUrl.isNullOrBlank()) {
                enabledExplore = false
            }
            ruleSearch = SearchRule(
                bookList = toNewRule(jsonItem.readString("ruleSearchList")),
                name = toNewRule(jsonItem.readString("ruleSearchName")),
                author = toNewRule(jsonItem.readString("ruleSearchAuthor")),
                intro = toNewRule(jsonItem.readString("ruleSearchIntroduce")),
                kind = toNewRule(jsonItem.readString("ruleSearchKind")),
                bookUrl = toNewRule(jsonItem.readString("ruleSearchNoteUrl")),
                coverUrl = toNewRule(jsonItem.readString("ruleSearchCoverUrl")),
                lastChapter = toNewRule(jsonItem.readString("ruleSearchLastChapter"))
            )
            ruleExplore = ExploreRule(
                bookList = toNewRule(jsonItem.readString("ruleFindList")),
                name = toNewRule(jsonItem.readString("ruleFindName")),
                author = toNewRule(jsonItem.readString("ruleFindAuthor")),
                intro = toNewRule(jsonItem.readString("ruleFindIntroduce")),
                kind = toNewRule(jsonItem.readString("ruleFindKind")),
                bookUrl = toNewRule(jsonItem.readString("ruleFindNoteUrl")),
                coverUrl = toNewRule(jsonItem.readString("ruleFindCoverUrl")),
                lastChapter = toNewRule(jsonItem.readString("ruleFindLastChapter"))
            )
            ruleBookInfo = BookInfoRule(
                init = toNewRule(jsonItem.readString("ruleBookInfoInit")),
                name = toNewRule(jsonItem.readString("ruleBookName")),
                author = toNewRule(jsonItem.readString("ruleBookAuthor")),
                intro = toNewRule(jsonItem.readString("ruleIntroduce")),
                kind = toNewRule(jsonItem.readString("ruleBookKind")),
                coverUrl = toNewRule(jsonItem.readString("ruleCoverUrl")),
                lastChapter = toNewRule(jsonItem.readString("ruleBookLastChapter")),
                tocUrl = toNewRule(jsonItem.readString("ruleChapterUrl"))
            )
            ruleToc = TocRule(
                chapterList = toNewRule(jsonItem.readString("ruleChapterList")),
                chapterName = toNewRule(jsonItem.readString("ruleChapterName")),
                chapterUrl = toNewRule(jsonItem.readString("ruleContentUrl")),
                nextTocUrl = toNewRule(jsonItem.readString("ruleChapterUrlNext"))
            )
            var content = toNewRule(jsonItem.readString("ruleBookContent")) ?: ""
            if (content.startsWith("$") && !content.startsWith("$.")) {
                content = content.substring(1)
            }
            ruleContent = ContentRule(
                content = content,
                replaceRegex = toNewRule(jsonItem.readString("ruleBookContentReplace")),
                nextContentUrl = toNewRule(jsonItem.readString("ruleContentUrlNext"))
            )
        }
    }


    // default规则适配
    // #正则#替换内容 替换成 ##正则##替换内容
    // | 替换成 ||
    // & 替换成 &&
    private fun toNewRule(oldRule: String?): String? {
        if (oldRule.isNullOrBlank()) return null
        var newRule = oldRule
        var reverse = false
        var allinone = false
        if (oldRule.startsWith("-")) {
            reverse = true
            newRule = oldRule.substring(1)
        }
        if (newRule.startsWith("+")) {
            allinone = true
            newRule = newRule.substring(1)
        }
        if (!newRule.startsWith("@CSS:", true) &&
            !newRule.startsWith("@XPath:", true) &&
            !newRule.startsWith("//") &&
            !newRule.startsWith("##") &&
            !newRule.startsWith(":") &&
            !newRule.contains("@js:", true) &&
            !newRule.contains("<js>", true)
        ) {
            if (newRule.contains("#") && !newRule.contains("##")) {
                newRule = oldRule.replace("#", "##")
            }
            if (newRule.contains("|") && !newRule.contains("||")) {
                if (newRule.contains("##")) {
                    val list = newRule.split("##")
                    if (list[0].contains("|")) {
                        newRule = list[0].replace("|", "||")
                        for (i in 1 until list.size) {
                            newRule += "##" + list[i]
                        }
                    }
                } else {
                    newRule = newRule.replace("|", "||")
                }
            }
            if (newRule.contains("&")
                && !newRule.contains("&&")
                && !newRule.contains("http")
                && !newRule.startsWith("/")
            ) {
                newRule = newRule.replace("&", "&&")
            }
        }
        if (allinone) {
            newRule = "+$newRule"
        }
        if (reverse) {
            newRule = "-$newRule"
        }
        return newRule
    }

    private fun toNewUrls(oldUrls: String?): String? {
        if (oldUrls.isNullOrBlank()) return null
        if (oldUrls.startsWith("@js:") || oldUrls.startsWith("<js>")) {
            return oldUrls
        }
        if (!oldUrls.contains("\n") && !oldUrls.contains("&&")) {
            return toNewUrl(oldUrls)
        }
        val urls = oldUrls.split("(&&|\r?\n)+".toRegex())
        return urls.map {
            toNewUrl(it)?.replace("\n\\s*".toRegex(), "")
        }.joinToString("\n")
    }

    private fun toNewUrl(oldUrl: String?): String? {
        if (oldUrl.isNullOrBlank()) return null
        var url: String = oldUrl
        if (oldUrl.startsWith("<js>", true)) {
            url = url.replace("=searchKey", "={{key}}")
                .replace("=searchPage", "={{page}}")
            return url
        }
        val map = HashMap<String, String>()
        var mather = headerPattern.matcher(url)
        if (mather.find()) {
            val header = mather.group()
            url = url.replace(header, "")
            map["headers"] = header.substring(8)
        }
        var urlList = url.split("|")
        url = urlList[0]
        if (urlList.size > 1) {
            map["charset"] = urlList[1].split("=")[1]
        }
        mather = jsPattern.matcher(url)
        val jsList = arrayListOf<String>()
        while (mather.find()) {
            jsList.add(mather.group())
            url = url.replace(jsList.last(), "$${jsList.size - 1}")
        }
        url = url.replace("{", "<").replace("}", ">")
        url = url.replace("searchKey", "{{key}}")
        url = url.replace("<searchPage([-+]1)>".toRegex(), "{{page$1}}")
            .replace("searchPage([-+]1)".toRegex(), "{{page$1}}")
            .replace("searchPage", "{{page}}")
        for ((index, item) in jsList.withIndex()) {
            url = url.replace(
                "$$index",
                item.replace("searchKey", "key").replace("searchPage", "page")
            )
        }
        urlList = url.split("@")
        url = urlList[0]
        if (urlList.size > 1) {
            map["method"] = "POST"
            map["body"] = urlList[1]
        }
        if (map.size > 0) {
            url += "," + GSON.toJson(map)
        }
        return url
    }

    private fun uaToHeader(ua: String?): String? {
        if (ua.isNullOrEmpty()) return null
        val map = mapOf(Pair(AppConst.UA_NAME, ua))
        return GSON.toJson(map)
    }

}