package io.legado.app.ui.book.read.content

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.FileUtils
import io.legado.app.utils.externalFiles
import io.legado.app.utils.getFile
import io.legado.app.utils.LogUtils
import splitties.init.appCtx
import java.io.File
import java.util.regex.Pattern

object ZhanweifuBookHelp {
    private val downloadDir: File = appCtx.externalFiles
    private const val cacheFolderName = "zhanweifu_book_cache"

    fun zhanweifuSaveText(
        book: Book,
        bookChapter: BookChapter,
        content: String
    ) {
        if (content.isEmpty()) return
        FileUtils.createFileIfNotExist(
            downloadDir,
            cacheFolderName,
            book.getFolderName(),
            bookChapter.getFileName(),
        ).writeText(content)
    }

    fun zhanweifuGetContent(book: Book, bookChapter: BookChapter): String? {
        val file = downloadDir.getFile(
            cacheFolderName,
            book.getFolderName(),
            bookChapter.getFileName()
        )
        if (file.exists()) {
            val string = file.readText()
            if (string.isEmpty()) {
                return null
            }
            return string
        }
        return null
    }

    fun zhanweifuDelContent(book: Book, bookChapter: BookChapter) {
        FileUtils.createFileIfNotExist(
            downloadDir,
            cacheFolderName,
            book.getFolderName(),
            bookChapter.getFileName()
        ).delete()
    }

    fun getAiSummaryFromCache(book: Book, chapter: BookChapter): String? {
        val cachePathUriString = AppConfig.aiSummaryCachePath ?: return null
        try {
            val cacheUri = Uri.parse(cachePathUriString)
            val pickedDir = DocumentFile.fromTreeUri(appCtx, cacheUri) ?: return null
            val bookFile = pickedDir.findFile("${book.name}.txt") ?: return null

            if (bookFile.exists() && bookFile.isFile) {
                appCtx.contentResolver.openInputStream(bookFile.uri)?.use {
                    val fileContent = it.reader().readText()
                    val pattern = Pattern.compile("--- ${Pattern.quote(chapter.title)} ---(.*?)(?=---|$)", Pattern.DOTALL)
                    val matcher = pattern.matcher(fileContent)
                    if (matcher.find()) {
                        val summary = matcher.group(1)?.trim()
                        if (!summary.isNullOrEmpty()) {
                            return summary
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LogUtils.e("getAiSummaryFromCache", "Error reading cache: ${e.message}\n${e.stackTraceToString()}")
        }
        return null
    }
}
