package io.legado.app.help

import com.github.liuyueyi.quick.transfer.ChineseUtils
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
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
        chapter: BookChapter, //已经经过简繁转换
        content: String,
        includeTitle: Boolean = true,
        useReplace: Boolean = true,
        chineseConvert: Boolean = true,
        reSegment: Boolean = true
    ): List<String> {
        //去除无效内容
        var mContent = content.trimStart {
            it.code <= 0x20 || it == '　' || it == ',' || it == '，'
        }
        //去除重复标题
        val titleRegex =
            "^(\\s|\\pP)*《*(${book.name})*》*(\\s|\\pP)*${chapter.title}(\\s|\\pP)+".toRegex()
        mContent = mContent.replace(titleRegex, "")
        if (includeTitle) {
            //重新添加标题
            mContent = chapter.getDisplayTitle() + "\n" + mContent
        }
        if (useReplace && book.getUseReplaceRule()) {
            //替换
            getReplaceRules().forEach { item ->
                if (item.pattern.isNotEmpty()) {
                    try {
                        mContent = if (item.isRegex) {
                            mContent.replace(item.pattern.toRegex(), item.replacement)
                        } else {
                            mContent.replace(item.pattern, item.replacement)
                        }
                    } catch (e: Exception) {
                        appCtx.toastOnUi("${item.name}替换出错")
                    }
                }
            }
        }
        if (reSegment && book.getReSegment()) {
            //重新分段
            mContent = ContentHelp.reSegment(mContent, chapter.title)
        }
        if (chineseConvert) {
            //简繁转换
            try {
                when (AppConfig.chineseConverterType) {
                    1 -> mContent = ChineseUtils.t2s(mContent)
                    2 -> mContent = ChineseUtils.s2t(mContent)
                }
            } catch (e: Exception) {
                appCtx.toastOnUi("简繁转换出错")
            }
        }
        val contents = arrayListOf<String>()
        mContent.split("\n").forEach { str ->
            val paragraph = str.trim {
                it.code <= 0x20 || it == '　'
            }
            if (paragraph.isNotEmpty()) {
                if (contents.isEmpty() && includeTitle) {
                    contents.add(paragraph)
                } else {
                    contents.add("${ReadBookConfig.paragraphIndent}$paragraph")
                }
            }
        }
        return contents
    }

}