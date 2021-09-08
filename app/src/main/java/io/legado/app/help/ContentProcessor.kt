package io.legado.app.help

import com.github.liuyueyi.quick.transfer.ChineseUtils
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.utils.toastOnUi
import splitties.init.appCtx
import java.lang.ref.WeakReference

class ContentProcessor private constructor(
    private val bookName: String,
    private val bookOrigin: String
) {

    companion object {
        private val processors = hashMapOf<String, WeakReference<ContentProcessor>>()

        fun get(bookName: String, bookOrigin: String): ContentProcessor {
            val processorWr = processors[bookName + bookOrigin]
            var processor: ContentProcessor? = processorWr?.get()
            if (processor == null) {
                processor = ContentProcessor(bookName, bookOrigin)
                processors[bookName + bookOrigin] = WeakReference(processor)
            }
            return processor
        }

        fun upReplaceRules() {
            processors.forEach {
                it.value.get()?.upReplaceRules()
            }
        }

    }

    private val replaceRules = arrayListOf<ReplaceRule>()

    init {
        upReplaceRules()
    }

    @Synchronized
    fun upReplaceRules() {
        replaceRules.clear()
        replaceRules.addAll(appDb.replaceRuleDao.findEnabledByScope(bookName, bookOrigin))
    }

    @Synchronized
    fun getReplaceRules(): Array<ReplaceRule> {
        return replaceRules.toTypedArray()
    }

    fun getContent(
        book: Book,
        title: String, //已经经过简繁转换
        content: String,
        includeTitle: Boolean = true,
        useReplace: Boolean = true,
        chineseConvert: Boolean = true,
        reSegment: Boolean = true
    ): List<String> {
        var content1 = content
        if (useReplace && book.getUseReplaceRule()) {
            getReplaceRules().forEach { item ->
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
        if (reSegment && book.getReSegment()) {
            content1 = ContentHelp.reSegment(content1, title)
        }
        if (chineseConvert) {
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
        content1.split("\n").forEach { str ->
            val paragraph = str.replace("^[\\n\\r]+".toRegex(), "").trim()
            if (contents.isEmpty()) {
                if (includeTitle) {
                    contents.add(title)
                }
                if (paragraph != title && paragraph.isNotEmpty()) {
                    contents.add("${ReadBookConfig.paragraphIndent}$paragraph")
                }
            } else if (paragraph.isNotEmpty()) {
                contents.add("${ReadBookConfig.paragraphIndent}$paragraph")
            }
        }
        return contents
    }

}