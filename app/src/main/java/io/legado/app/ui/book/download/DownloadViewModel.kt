package io.legado.app.ui.book.download

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.App
import io.legado.app.R
import io.legado.app.constant.AppPattern
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.Book
import io.legado.app.help.BookHelp
import io.legado.app.utils.*
import java.io.File


class DownloadViewModel(application: Application) : BaseViewModel(application) {


    fun export(path: String, book: Book, finally: (msg: String) -> Unit) {
        execute {
            if (path.isContentPath()) {
                val uri = Uri.parse(path)
                DocumentFile.fromTreeUri(context, uri)?.let {
                    export(it, book)
                }
            } else {
                export(FileUtils.createFolderIfNotExist(path), book)
            }
        }.onError {
            finally(it.localizedMessage ?: "ERROR")
        }.onSuccess {
            finally(context.getString(R.string.success))
        }
    }

    private fun export(doc: DocumentFile, book: Book) {
        DocumentUtils.createFileIfNotExist(doc, "${book.name} 作者:${book.author}.txt")
            ?.writeText(context, getAllContents(book))
        App.db.bookChapterDao().getChapterList(book.bookUrl).forEach { chapter ->
            BookHelp.getContent(book, chapter).let { content ->
                content?.split("\n")?.forEachIndexed { index, text ->
                    val matcher = AppPattern.imgPattern.matcher(text)
                    if (matcher.find()) {
                        var src = matcher.group(1)
                        src = NetworkUtils.getAbsoluteURL(chapter.url, src)
                        src?.let {
                            val vfile = BookHelp.getImage(book, src)
                            if(vfile.exists()) {
                                DocumentUtils.createFileIfNotExist(doc, "${index}-${MD5Utils.md5Encode16(src)}.jpg", subDirs = arrayOf("${book.name}_${book.author}", "images", chapter.title))
                                    ?.writeBytes(context, vfile.readBytes())
                            }
                        }
                    }
                }
            }
        }
    }

    private fun export(file: File, book: Book) {
        FileUtils.createFileIfNotExist(file, "${book.name} 作者:${book.author}.txt")
            .writeText(getAllContents(book))
        App.db.bookChapterDao().getChapterList(book.bookUrl).forEach { chapter ->
            BookHelp.getContent(book, chapter).let { content ->
                content?.split("\n")?.forEachIndexed { index, text ->
                    val matcher = AppPattern.imgPattern.matcher(text)
                    if (matcher.find()) {
                        var src = matcher.group(1)
                        src = NetworkUtils.getAbsoluteURL(chapter.url, src)
                        src?.let {
                            val vfile = BookHelp.getImage(book, src)
                            if(vfile.exists()) {
                                FileUtils.createFileIfNotExist(file, "${book.name}_${book.author}", "images", chapter.title, "${index}-${MD5Utils.md5Encode16(src)}.jpg")
                                    .writeBytes(vfile.readBytes())
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getAllContents(book: Book): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append(book.name)
            .append("\n")
            .append(context.getString(R.string.author_show, book.author))
        App.db.bookChapterDao().getChapterList(book.bookUrl).forEach { chapter ->
            BookHelp.getContent(book, chapter).let {
                stringBuilder.append("\n\n")
                    .append(chapter.title)
                    .append("\n")
                    .append(it)
            }
        }
        return stringBuilder.toString()
    }
}