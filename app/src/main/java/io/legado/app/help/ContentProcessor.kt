package io.legado.app.help

import com.github.liuyueyi.quick.transfer.ChineseUtils
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.utils.toastOnUi
import splitties.init.appCtx

class ContentProcessor(private val bookName: String, private val bookOrigin: String) {

    private var replaceRules = arrayListOf<ReplaceRule>()

    init {
        upReplaceRules()
    }

    @Synchronized
    fun upReplaceRules() {
        replaceRules.clear()
        replaceRules.addAll(appDb.replaceRuleDao.findEnabledByScope(bookName, bookOrigin))
    }

    @Synchronized
    fun getContent(
        book: Book,
        title: String, //已经经过简繁转换
        content: String,
        isRead: Boolean = true,
        useReplace: Boolean = book.getUseReplaceRule()
    ): List<String> {
        var content1 = content
        if (useReplace) {
            replaceRules.forEach { item ->
                if (item.pattern.isNotEmpty()) {
                    try {
                        content1 = if (item.isRegex) {
                            content1.replace(item.pattern.toRegex(), item.replacement)
                        } else {
                            content1.replace(item.pattern, item.replacement)
                        }
                    } catch (e: Exception) {
                        appCtx.toastOnUi("${item.name}替换出错")
                    }
                }
            }
        }
        if (isRead) {
            if (book.getReSegment()) {
                content1 = ContentHelp.reSegment(content1, title)
            }
            try {
                when (AppConfig.chineseConverterType) {
                    1 -> content1 = ChineseUtils.t2s(content1)
                    2 -> content1 = ChineseUtils.s2t(content1)
                }
            } catch (e: Exception) {
                appCtx.toastOnUi("简繁转换出错")
            }
        }
        val contents = arrayListOf<String>()
        content1.split("\n").forEach {
            val str = it.replace("^[\\n\\r]+".toRegex(), "").trim()
            if (contents.isEmpty()) {
                contents.add(title)
                if (str != title && str.isNotEmpty()) {
                    contents.add("${ReadBookConfig.paragraphIndent}$str")
                }
            } else if (str.isNotEmpty()) {
                contents.add("${ReadBookConfig.paragraphIndent}$str")
            }
        }
        return contents
    }

}