package io.legado.app.help.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.App
import io.legado.app.data.entities.BookSource
import io.legado.app.utils.DocumentUtils
import io.legado.app.utils.FileUtils
import io.legado.app.utils.isContentScheme
import org.jetbrains.anko.toast
import java.io.File

object ImportOldData {

    fun importUri(context: Context, uri: Uri) {
        if (uri.isContentScheme()) {
            DocumentFile.fromTreeUri(context, uri)?.listFiles()?.forEach { doc ->
                when (doc.name) {
                    "myBookShelf.json" ->
                        kotlin.runCatching {
                            DocumentUtils.readText(context, doc.uri)?.let { json ->
                                val importCount = importOldBookshelf(json)
                                context.toast("成功导入书籍${importCount}")
                            }
                        }.onFailure {
                            context.toast("导入书籍失败\n${it.localizedMessage}")
                        }
                    "myBookSource.json" ->
                        kotlin.runCatching {
                            DocumentUtils.readText(context, doc.uri)?.let { json ->
                                val importCount = importOldSource(json)
                                context.toast("成功导入书源${importCount}")
                            }
                        }.onFailure {
                            context.toast("导入源失败\n${it.localizedMessage}")
                        }
                    "myBookReplaceRule.json" ->
                        kotlin.runCatching {
                            DocumentUtils.readText(context, doc.uri)?.let { json ->
                                val importCount = importOldReplaceRule(json)
                                context.toast("成功导入替换规则${importCount}")
                            }
                        }.onFailure {
                            context.toast("导入替换规则失败\n${it.localizedMessage}")
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
                    context.toast("成功导入书籍${importCount}")
                }.onFailure {
                    context.toast("导入书籍失败\n${it.localizedMessage}")
                }

                kotlin.runCatching {// Book source
                    val sourceFile =
                        FileUtils.getFile(file, "myBookSource.json")
                    val json = sourceFile.readText()
                    val importCount = importOldSource(json)
                    context.toast("成功导入书源${importCount}")
                }.onFailure {
                    context.toast("导入源失败\n${it.localizedMessage}")
                }

                kotlin.runCatching {// Replace rules
                    val ruleFile = FileUtils.getFile(file, "myBookReplaceRule.json")
                    if (ruleFile.exists()) {
                        val json = ruleFile.readText()
                        val importCount = importOldReplaceRule(json)
                        context.toast("成功导入替换规则${importCount}")
                    } else {
                        context.toast("未找到替换规则")
                    }
                }.onFailure {
                    context.toast("导入替换规则失败\n${it.localizedMessage}")
                }
            }
        }
    }

    private fun importOldBookshelf(json: String): Int {
        val books = OldBook.toNewBook(json)
        App.db.bookDao.insert(*books.toTypedArray())
        return books.size
    }

    private fun importOldSource(json: String): Int {
        val bookSources = mutableListOf<BookSource>()
        val items: List<Map<String, Any>> = Restore.jsonPath.parse(json).read("$")
        for (item in items) {
            val jsonItem = Restore.jsonPath.parse(item)
            OldRule.jsonToBookSource(jsonItem.jsonString())?.let {
                bookSources.add(it)
            }
        }
        App.db.bookSourceDao.insert(*bookSources.toTypedArray())
        return bookSources.size
    }

    private fun importOldReplaceRule(json: String): Int {
        val rules = OldReplace.jsonToReplaceRules(json)
        App.db.replaceRuleDao.insert(*rules.toTypedArray())
        return rules.size
    }
}