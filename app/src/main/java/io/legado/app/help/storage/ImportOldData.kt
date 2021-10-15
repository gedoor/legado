package io.legado.app.help.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.data.appDb
import io.legado.app.help.BookSourceAnalyzer
import io.legado.app.utils.*
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
                                context.toastOnUi("成功导入书籍${importCount}")
                            }
                        }.onFailure {
                            context.toastOnUi("导入书籍失败\n${it.localizedMessage}")
                        }
                    "myBookSource.json" ->
                        kotlin.runCatching {
                            DocumentUtils.readText(context, doc.uri)?.let { json ->
                                val importCount = importOldSource(json)
                                context.toastOnUi("成功导入书源${importCount}")
                            }
                        }.onFailure {
                            context.toastOnUi("导入源失败\n${it.localizedMessage}")
                        }
                    "myBookReplaceRule.json" ->
                        kotlin.runCatching {
                            DocumentUtils.readText(context, doc.uri)?.let { json ->
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
        val books = OldBook.toNewBook(json)
        appDb.bookDao.insert(*books.toTypedArray())
        return books.size
    }

    fun importOldSource(json: String): Int {
        val bookSources = BookSourceAnalyzer.jsonToBookSources(json)
        appDb.bookSourceDao.insert(*bookSources.toTypedArray())
        return bookSources.size
    }

    private fun importOldReplaceRule(json: String): Int {
        val rules = OldReplace.jsonToReplaceRules(json)
        appDb.replaceRuleDao.insert(*rules.toTypedArray())
        return rules.size
    }
}