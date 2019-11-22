package io.legado.app.help.storage

import io.legado.app.constant.AppConst
import io.legado.app.constant.BookType
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.*
import io.legado.app.help.storage.Restore.jsonPath
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.readInt
import io.legado.app.utils.readString
import java.util.regex.Pattern

object OldRule {
    private val headerPattern = Pattern.compile("@Header:\\{.+?\\}", Pattern.CASE_INSENSITIVE)
    private val jsPattern = Pattern.compile("\\{\\{.+?\\}\\}", Pattern.CASE_INSENSITIVE)

    fun jsonToBookSource(json: String): BookSource? {
        var source: BookSource? = null
        runCatching {
            source = GSON.fromJsonObject<BookSource>(json.trim())
        }
        runCatching {
            if (source == null || source?.searchUrl.isNullOrBlank()) {
                source = BookSource().apply {
                    val jsonItem = jsonPath.parse(json.trim())
                    bookSourceUrl = jsonItem.readString("bookSourceUrl") ?: ""
                    bookSourceName = jsonItem.readString("bookSourceName") ?: ""
                    bookSourceGroup = jsonItem.readString("bookSourceGroup") ?: ""
                    loginUrl = jsonItem.readString("loginUrl")
                    bookUrlPattern = jsonItem.readString("ruleBookUrlPattern")
                    customOrder = jsonItem.readInt("serialNumber") ?: 0
                    header = uaToHeader(jsonItem.readString("httpUserAgent"))
                    searchUrl = toNewUrl(jsonItem.readString("ruleSearchUrl"))
                    exploreUrl = toNewUrls(jsonItem.readString("ruleFindUrl"))
                    bookSourceType =
                        if (jsonItem.readString("bookSourceType") == "AUDIO") BookType.audio else BookType.default
                    if (exploreUrl.isNullOrBlank()) {
                        enabledExplore = false
                    }
                    val searchRule = SearchRule(
                        bookList = jsonItem.readString("ruleSearchList"),
                        name = jsonItem.readString("ruleSearchName"),
                        author = jsonItem.readString("ruleSearchAuthor"),
                        intro = jsonItem.readString("ruleSearchIntroduce"),
                        kind = jsonItem.readString("ruleSearchKind"),
                        bookUrl = jsonItem.readString("ruleSearchNoteUrl"),
                        coverUrl = jsonItem.readString("ruleSearchCoverUrl"),
                        lastChapter = jsonItem.readString("ruleSearchLastChapter")
                    )
                    ruleSearch = GSON.toJson(searchRule)
                    val exploreRule = ExploreRule(
                        bookList = jsonItem.readString("ruleFindList"),
                        name = jsonItem.readString("ruleFindName"),
                        author = jsonItem.readString("ruleFindAuthor"),
                        intro = jsonItem.readString("ruleFindIntroduce"),
                        kind = jsonItem.readString("ruleFindKind"),
                        bookUrl = jsonItem.readString("ruleFindNoteUrl"),
                        coverUrl = jsonItem.readString("ruleFindCoverUrl"),
                        lastChapter = jsonItem.readString("ruleFindLastChapter")
                    )
                    ruleExplore = GSON.toJson(exploreRule)
                    val bookInfoRule = BookInfoRule(
                        init = jsonItem.readString("ruleBookInfoInit"),
                        name = jsonItem.readString("ruleBookName"),
                        author = jsonItem.readString("ruleBookAuthor"),
                        intro = jsonItem.readString("ruleIntroduce"),
                        kind = jsonItem.readString("ruleBookKind"),
                        coverUrl = jsonItem.readString("ruleCoverUrl"),
                        lastChapter = jsonItem.readString("ruleBookLastChapter"),
                        tocUrl = jsonItem.readString("ruleChapterUrl")
                    )
                    ruleBookInfo = GSON.toJson(bookInfoRule)
                    val chapterRule = TocRule(
                        chapterList = jsonItem.readString("ruleChapterList"),
                        chapterName = jsonItem.readString("ruleChapterName"),
                        chapterUrl = jsonItem.readString("ruleContentUrl"),
                        nextTocUrl = jsonItem.readString("ruleChapterUrlNext")
                    )
                    ruleToc = GSON.toJson(chapterRule)
                    var content = jsonItem.readString("ruleBookContent") ?: ""
                    if (content.startsWith("$") && !content.startsWith("$.")) {
                        content = content.substring(1)
                    }
                    val contentRule = ContentRule(
                        content = content,
                        nextContentUrl = jsonItem.readString("ruleContentUrlNext")
                    )
                    ruleContent = GSON.toJson(contentRule)
                }
            }
        }
        return source
    }

    private fun toNewUrls(oldUrl: String?): String? {
        if (oldUrl == null) return null
        if (!oldUrl.contains("\n") && !oldUrl.contains("&&"))
            return toNewUrl(oldUrl)

        val urls = oldUrl.split("(&&|\n)+".toRegex())
        var newUrl = ""
        for (url in urls) {
            newUrl += toNewUrl(url)?.replace("\\n\\s*".toRegex(), "") + "\n"
        }
        return newUrl
    }

    private fun toNewUrl(oldUrl: String?): String? {
        if (oldUrl == null) return null
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