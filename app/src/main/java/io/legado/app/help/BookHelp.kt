package io.legado.app.help

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.App
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.utils.*
import org.apache.commons.text.similarity.JaccardSimilarity
import java.io.File
import kotlin.math.min

object BookHelp {
    private const val cacheFolderName = "book_cache"
    private var downloadPath: String =
        App.INSTANCE.getPrefString(PreferKey.downloadPath)
            ?: App.INSTANCE.getExternalFilesDir(null)?.absolutePath
            ?: App.INSTANCE.cacheDir.absolutePath

    fun upDownloadPath() {
        downloadPath =
            App.INSTANCE.getPrefString(PreferKey.downloadPath)
                ?: App.INSTANCE.getExternalFilesDir(null)?.absolutePath
                        ?: App.INSTANCE.cacheDir.absolutePath
    }

    private val downloadUri get() = Uri.parse(downloadPath)

    private fun bookFolderName(book: Book): String {
        return formatFolderName(book.name) + MD5Utils.md5Encode16(book.bookUrl)
    }

    private fun bookChapterName(bookChapter: BookChapter): String {
        return String.format("%05d-%s", bookChapter.index, MD5Utils.md5Encode(bookChapter.title))
    }

    private fun getBookCachePath(): String {
        return "$downloadPath${File.separator}$cacheFolderName"
    }

    fun clearCache() {
        if (downloadUri.isDocumentUri(App.INSTANCE)) {
            DocumentFile.fromTreeUri(App.INSTANCE, downloadUri)
                ?.findFile(cacheFolderName)
                ?.delete()
        } else {
            FileUtils.deleteFile(getBookCachePath())
            FileUtils.createFolderIfNotExist(getBookCachePath())
        }
    }

    @Synchronized
    fun saveContent(book: Book, bookChapter: BookChapter, content: String) {
        if (content.isEmpty()) return
        if (downloadUri.isDocumentUri(App.INSTANCE)) {
            DocumentFile.fromTreeUri(App.INSTANCE, downloadUri)?.let {
                DocumentUtils.createFileIfNotExist(
                    it,
                    "${bookChapterName(bookChapter)}.nb",
                    subDirs = *arrayOf(cacheFolderName, bookFolderName(book))
                )?.uri?.writeText(App.INSTANCE, content)
            }
        } else {
            FileUtils.createFileIfNotExist(
                File(downloadPath),
                subDirs = *arrayOf(cacheFolderName, bookFolderName(book))
            ).listFiles()?.forEach {
                if (it.name.startsWith(String.format("%05d", bookChapter.index))) {
                    it.delete()
                    return@forEach
                }
            }
            FileUtils.createFileIfNotExist(
                File(downloadPath),
                "${bookChapterName(bookChapter)}.nb",
                subDirs = *arrayOf(cacheFolderName, bookFolderName(book))
            ).writeText(content)
        }
    }

    fun getChapterCount(book: Book): Int {
        if (downloadUri.isDocumentUri(App.INSTANCE)) {
            DocumentFile.fromTreeUri(App.INSTANCE, downloadUri)?.let {
                return DocumentUtils.createFolderIfNotExist(
                    it,
                    subDirs = *arrayOf(cacheFolderName, bookFolderName(book))
                )?.listFiles()?.size ?: 0
            }
        } else {
            return FileUtils.createFileIfNotExist(
                File(downloadPath),
                subDirs = *arrayOf(cacheFolderName, bookFolderName(book))
            ).list()?.size ?: 0
        }
        return 0
    }

    fun hasContent(book: Book, bookChapter: BookChapter): Boolean {
        if (downloadUri.isDocumentUri(App.INSTANCE)) {
            DocumentFile.fromTreeUri(App.INSTANCE, downloadUri)?.let {
                return DocumentUtils.exists(
                    it,
                    "${bookChapterName(bookChapter)}.nb",
                    subDirs = *arrayOf(cacheFolderName, bookFolderName(book))
                )
            }
        } else {
            return FileUtils.exists(
                File(downloadPath),
                "${bookChapterName(bookChapter)}.nb",
                subDirs = *arrayOf(cacheFolderName, bookFolderName(book))
            )
        }
        return false
    }

    fun getContent(book: Book, bookChapter: BookChapter): String? {
        if (downloadUri.isDocumentUri(App.INSTANCE)) {
            DocumentFile.fromTreeUri(App.INSTANCE, downloadUri)?.let {
                return DocumentUtils.createFileIfNotExist(
                    it,
                    "${bookChapterName(bookChapter)}.nb",
                    subDirs = *arrayOf(cacheFolderName, bookFolderName(book))
                )?.uri?.readText(App.INSTANCE)
            }
        } else {
            return FileUtils.createFileIfNotExist(
                File(downloadPath),
                "${bookChapterName(bookChapter)}.nb",
                subDirs = *arrayOf(cacheFolderName, bookFolderName(book))
            ).readText()
        }
        return null
    }

    fun delContent(book: Book, bookChapter: BookChapter) {
        if (downloadUri.isDocumentUri(App.INSTANCE)) {
            DocumentFile.fromTreeUri(App.INSTANCE, downloadUri)?.let {
                DocumentUtils.getDirDocument(
                    it,
                    subDirs = *arrayOf(cacheFolderName, bookFolderName(book))
                )?.findFile("${bookChapterName(bookChapter)}.nb")
                    ?.delete()
            }
        } else {
            FileUtils.createFileIfNotExist(
                File(downloadPath),
                "${bookChapterName(bookChapter)}.nb",
                subDirs = *arrayOf(cacheFolderName, bookFolderName(book))
            ).delete()
        }
    }

    private fun formatFolderName(folderName: String): String {
        return folderName.replace("[\\\\/:*?\"<>|.]".toRegex(), "")
    }

    fun formatAuthor(author: String?): String {
        return author
            ?.replace("作\\s*者[\\s:：]*".toRegex(), "")
            ?.replace("\\s+".toRegex(), " ")
            ?.trim { it <= ' ' }
            ?: ""
    }

    /**
     * 找到相似度最高的章节
     */
    fun getDurChapterIndexByChapterTitle(
        title: String?,
        index: Int,
        chapters: List<BookChapter>
    ): Int {
        if (title.isNullOrEmpty()) {
            return min(index, chapters.lastIndex)
        }
        if (chapters.size > index && title == chapters[index].title) {
            return index
        }

        var newIndex = 0
        val jSimilarity = JaccardSimilarity()
        var similarity = if (chapters.size > index) {
            jSimilarity.apply(title, chapters[index].title)
        } else 0.0
        if (similarity == 1.0) {
            return index
        } else {
            for (i in 1..50) {
                if (index - i in chapters.indices) {
                    jSimilarity.apply(title, chapters[index - i].title).let {
                        if (it > similarity) {
                            similarity = it
                            newIndex = index - i
                            if (similarity == 1.0) {
                                return newIndex
                            }
                        }
                    }
                }
                if (index + i in chapters.indices) {
                    jSimilarity.apply(title, chapters[index + i].title).let {
                        if (it > similarity) {
                            similarity = it
                            newIndex = index + i
                            if (similarity == 1.0) {
                                return newIndex
                            }
                        }
                    }
                }
            }
        }
        return newIndex
    }

    var bookName: String? = null
    var bookOrigin: String? = null
    var replaceRules: List<ReplaceRule> = arrayListOf()

    fun disposeContent(
        title: String,
        name: String,
        origin: String?,
        content: String,
        enableReplace: Boolean
    ): String {
        var c = content
        synchronized(this) {
            if (enableReplace && (bookName != name || bookOrigin != origin)) {
                replaceRules = if (origin.isNullOrEmpty()) {
                    App.db.replaceRuleDao().findEnabledByScope(name)
                } else {
                    App.db.replaceRuleDao().findEnabledByScope(name, origin)
                }
            }
        }
        if (!content.substringBefore("\n").contains(title)) {
            c = title + "\n" + c
        }
        for (item in replaceRules) {
            item.pattern.let {
                if (it.isNotEmpty()) {
                    c = if (item.isRegex) {
                        c.replace(it.toRegex(), item.replacement)
                    } else {
                        c.replace(it, item.replacement)
                    }
                }
            }
        }
        val indent = App.INSTANCE.getPrefInt("textIndent", 2)
        return c.replace("\\s*\\n+\\s*".toRegex(), "\n" + "　".repeat(indent))
    }
}