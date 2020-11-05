package io.legado.app.help.storage

import androidx.annotation.Keep
import io.legado.app.constant.AppConst
import io.legado.app.constant.BookType
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.*
import io.legado.app.help.storage.Restore.jsonPath
import io.legado.app.utils.*
import java.util.regex.Pattern

@Suppress("RegExpRedundantEscape")
object OldRule {
    private val headerPattern = Pattern.compile("@Header:\\{.+?\\}", Pattern.CASE_INSENSITIVE)
    private val jsPattern = Pattern.compile("\\{\\{.+?\\}\\}", Pattern.CASE_INSENSITIVE)

    fun jsonToBookSource(json: String): BookSource? {
        val source = BookSource()
        val sourceAny = try {
            GSON.fromJsonObject<BookSourceAny>(json.trim())
        } catch (e: Exception) {
            null
        }
        try {
            if (sourceAny?.ruleToc == null) {
                source.apply {
                    val jsonItem = jsonPath.parse(json.trim())
                    bookSourceUrl = jsonItem.readString("bookSourceUrl") ?: ""
                    bookSourceName = jsonItem.readString("bookSourceName") ?: ""
                    bookSourceGroup = jsonItem.readString("bookSourceGroup")
                    loginUrl = jsonItem.readString("loginUrl")
                    bookSourceComment = jsonItem.readString("bookSourceComment") ?: ""
                    bookUrlPattern = jsonItem.readString("ruleBookUrlPattern")
                    customOrder = jsonItem.readInt("serialNumber") ?: 0
                    header = uaToHeader(jsonItem.readString("httpUserAgent"))
                    searchUrl = toNewUrl(jsonItem.readString("ruleSearchUrl"))
                    exploreUrl = toNewUrls(jsonItem.readString("ruleFindUrl"))
                    bookSourceType =
                        if (jsonItem.readString("bookSourceType") == "AUDIO") BookType.audio else BookType.default
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
            } else {
                source.bookSourceUrl = sourceAny.bookSourceUrl
                source.bookSourceName = sourceAny.bookSourceName
                source.bookSourceGroup = sourceAny.bookSourceGroup
                source.bookSourceType = sourceAny.bookSourceType
                source.bookUrlPattern = sourceAny.bookUrlPattern
                source.customOrder = sourceAny.customOrder
                source.enabled = sourceAny.enabled
                source.enabledExplore = sourceAny.enabledExplore
                source.header = sourceAny.header
                source.loginUrl = sourceAny.loginUrl
                source.bookSourceComment = sourceAny.bookSourceComment
                source.lastUpdateTime = sourceAny.lastUpdateTime
                source.weight = sourceAny.weight
                source.exploreUrl = sourceAny.exploreUrl
                source.ruleExplore = if (sourceAny.ruleExplore is String) {
                    GSON.fromJsonObject(sourceAny.ruleExplore as? String)
                } else {
                    GSON.fromJsonObject(GSON.toJson(sourceAny.ruleExplore))
                }
                source.searchUrl = sourceAny.searchUrl
                source.ruleSearch = if (sourceAny.ruleSearch is String) {
                    GSON.fromJsonObject(sourceAny.ruleSearch as? String)
                } else {
                    GSON.fromJsonObject(GSON.toJson(sourceAny.ruleSearch))
                }
                source.ruleBookInfo = if (sourceAny.ruleBookInfo is String) {
                    GSON.fromJsonObject(sourceAny.ruleBookInfo as? String)
                } else {
                    GSON.fromJsonObject(GSON.toJson(sourceAny.ruleBookInfo))
                }
                source.ruleToc = if (sourceAny.ruleToc is String) {
                    GSON.fromJsonObject(sourceAny.ruleToc as? String)
                } else {
                    GSON.fromJsonObject(GSON.toJson(sourceAny.ruleToc))
                }
                source.ruleContent = if (sourceAny.ruleContent is String) {
                    GSON.fromJsonObject(sourceAny.ruleContent as? String)
                } else {
                    GSON.fromJsonObject(GSON.toJson(sourceAny.ruleContent))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return source
    }

    @Keep
    data class BookSourceAny(
        var bookSourceName: String = "",                // 名称
        var bookSourceGroup: String? = null,            // 分组
        var bookSourceUrl: String = "",                 // 地址，包括 http/https
        var bookSourceType: Int = BookType.default,     // 类型，0 文本，1 音频
        var bookUrlPattern: String? = null,             // 详情页url正则
        var customOrder: Int = 0,                       // 手动排序编号
        var enabled: Boolean = true,                    // 是否启用
        var enabledExplore: Boolean = true,             // 启用发现
        var header: String? = null,                     // 请求头
        var loginUrl: String? = null,                   // 登录地址
        var bookSourceComment: String? = "",             //书源注释
        var lastUpdateTime: Long = 0,                   // 最后更新时间，用于排序
        var weight: Int = 0,                            // 智能排序的权重
        var exploreUrl: String? = null,                 // 发现url
        var ruleExplore: Any? = null,           // 发现规则
        var searchUrl: String? = null,                  // 搜索url
        var ruleSearch: Any? = null,             // 搜索规则
        var ruleBookInfo: Any? = null,         // 书籍信息页规则
        var ruleToc: Any? = null,                   // 目录页规则
        var ruleContent: Any? = null            // 正文页规则
    )

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
        if (!oldUrls.contains("\n") && !oldUrls.contains("&&"))
            return toNewUrl(oldUrls)

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
