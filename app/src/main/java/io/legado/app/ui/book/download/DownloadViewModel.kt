package io.legado.app.ui.book.download

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.Book
import io.legado.app.help.BookHelp
import io.legado.app.utils.DocumentUtils
import io.legado.app.utils.FileUtils
import io.legado.app.utils.isContentPath
import io.legado.app.utils.writeText
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
    }

    private fun export(file: File, book: Book) {
        FileUtils.createFileIfNotExist(file, "${book.name} 作者:${book.author}.txt")
            .writeText(getAllContents(book))
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