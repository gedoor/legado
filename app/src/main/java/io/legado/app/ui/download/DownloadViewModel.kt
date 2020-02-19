package io.legado.app.ui.download

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.Book
import io.legado.app.help.BookHelp
import io.legado.app.utils.FileUtils
import io.legado.app.utils.isContentPath
import java.io.File


class DownloadViewModel(application: Application) : BaseViewModel(application) {


    fun export(path: String, book: Book) {
        execute {
            if (path.isContentPath()) {
                val uri = Uri.parse(path)
                DocumentFile.fromTreeUri(context, uri)
                    ?.createFile("txt", book.name)
                    ?.let {
                        export(it.uri, book)
                    }
            } else {
                FileUtils.createFolderIfNotExist(path).let {
                    export(it, book)
                }
            }
        }.onError {
            toast(it.localizedMessage ?: "ERROR")
        }
    }


    private fun export(uri: Uri, book: Book) {

    }

    private fun export(file: File, book: Book) {

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