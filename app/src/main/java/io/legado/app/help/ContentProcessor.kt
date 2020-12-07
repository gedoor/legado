package io.legado.app.help

import com.hankcs.hanlp.HanLP
import io.legado.app.App
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.ReplaceRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast

class ContentProcessor(private val bookName: String, private val bookOrigin: String) {

    private var replaceRules = arrayListOf<ReplaceRule>()

    init {
        upReplaceRules()
    }

    @Synchronized
    fun upReplaceRules() {
        replaceRules.clear()
        replaceRules.addAll(App.db.replaceRuleDao.findEnabledByScope(bookName, bookOrigin))
    }

    suspend fun getContent(
        book: Book,
        title: String, //已经经过简繁转换
        content: String,
        isRead: Boolean = true
    ): List<String> {
        var content1 = content
        if (book.getUseReplaceRule()) {
            replaceRules.forEach { item ->
                if (item.pattern.isNotEmpty()) {
                    try {
                        content1 = if (item.isRegex) {
                            content1.replace(item.pattern.toRegex(), item.replacement)
                        } else {
                            content1.replace(item.pattern, item.replacement)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            App.INSTANCE.toast("${item.name}替换出错")
                        }
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
                    1 -> content1 = HanLP.convertToSimplifiedChinese(content1)
                    2 -> content1 = HanLP.convertToTraditionalChinese(content1)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    App.INSTANCE.toast("简繁转换出错")
                }
            }
        }
        val contents = arrayListOf<String>()
        content1.split("\n").forEach {
            val str = it.replace("^[\\n\\s\\r]+".toRegex(), "")
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