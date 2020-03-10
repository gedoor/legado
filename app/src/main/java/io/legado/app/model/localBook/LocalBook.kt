package io.legado.app.model.localBook

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.App
import io.legado.app.data.entities.Book
import io.legado.app.help.BookHelp
import io.legado.app.utils.FileUtils


object LocalBook {

    fun importFile(doc: DocumentFile) {
        doc.name?.let { fileName ->
            val str = fileName.substringBeforeLast(".")
            val authorIndex = str.indexOf("作者")
            var name: String
            var author: String
            if (authorIndex == -1) {
                name = str
                author = ""
            } else {
                name = str.substring(0, authorIndex)
                author = str.substring(authorIndex)
                author = BookHelp.formatAuthor(author)
            }
            val smhStart = name.indexOf("《")
            val smhEnd = name.indexOf("》")
            if (smhStart != -1 && smhEnd != -1) {
                name = (name.substring(smhStart + 1, smhEnd))
            }
            val book = Book(
                bookUrl = doc.uri.toString(),
                name = name,
                author = author,
                originName = fileName
            )
            App.db.bookDao().insert(book)
        }
    }

    fun deleteBook(book: Book, deleteOriginal: Boolean) {
        kotlin.runCatching {
            if (book.isTxt()) {
                val bookFile = FileUtils.getFile(AnalyzeTxtFile.cacheFolder, book.originName)
                bookFile.delete()
            }

            if (deleteOriginal) {
                val uri = Uri.parse(book.bookUrl)
                DocumentFile.fromSingleUri(App.INSTANCE, uri)?.delete()
            }
        }
    }
}