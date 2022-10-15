package io.legado.app.help.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.constant.BookType
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.help.ReplaceAnalyzer
import io.legado.app.utils.*
import java.io.File

object ImportOldData {

    fun importUri(context: Context, uri: Uri) {
        if (uri.isContentScheme()) {
            DocumentFile.fromTreeUri(context, uri)?.listFiles()?.forEach { doc ->
                when (doc.name) {
                    "myBookShelf.json" ->
                        kotlin.runCatching {
                            doc.uri.readText(context).let { json ->
                                val importCount = importOldBookshelf(json)
                                context.toastOnUi("成功导入书籍${importCount}")
                            }
                        }.onFailure {
                            context.toastOnUi("导入书籍失败\n${it.localizedMessage}")
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
                    context.toastOnUi("成功导入书籍${importCount}")
                }.onFailure {
                    context.toastOnUi("导入书籍失败\n${it.localizedMessage}")
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
        val count = BookSource.fromJsonArray(json).onSuccess {
            appDb.bookSourceDao.insert(*it.toTypedArray())
        }.getOrNull()?.size
        return count ?: 0
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
}