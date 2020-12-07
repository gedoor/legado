package io.legado.app.model.localBook

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.App
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.BookHelp
import io.legado.app.utils.*
import java.io.File


object LocalBook {
    private const val folderName = "bookTxt"
    val cacheFolder: File by lazy {
        val rootFile = App.INSTANCE.getExternalFilesDir(null)
            ?: App.INSTANCE.externalCacheDir
            ?: App.INSTANCE.cacheDir
        FileUtils.createFolderIfNotExist(rootFile, folderName)
    }

    fun getChapterList(book: Book): ArrayList<BookChapter> {
        return if (book.isEpub()) {
            EPUBFile.getChapterList(book)
        } else {
            AnalyzeTxtFile().analyze(book)
        }
    }

    fun getContext(book: Book, chapter: BookChapter): String? {
        return if (book.isEpub()) {
            EPUBFile.getContent(book, chapter)
        } else {
            AnalyzeTxtFile.getContent(book, chapter)
        }
    }

    fun importFile(uri: Uri): Book {
        val path: String
        val fileName = if (uri.isContentScheme()) {
            path = uri.toString()
            val doc = DocumentFile.fromSingleUri(App.INSTANCE, uri)
            doc?.let {
                val bookFile = FileUtils.getFile(cacheFolder, it.name!!)
                if (!bookFile.exists()) {
                    bookFile.createNewFile()
                    doc.readBytes(App.INSTANCE)?.let { bytes ->
                        bookFile.writeBytes(bytes)
                    }
                }
            }
            doc?.name!!
        } else {
            path = uri.path!!
            File(path).name
        }
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
            author = BookHelp.formatBookAuthor(author)
        }
        val smhStart = name.indexOf("《")
        val smhEnd = name.indexOf("》")
        if (smhStart != -1 && smhEnd != -1) {
            name = (name.substring(smhStart + 1, smhEnd))
        }
        val book = Book(
            bookUrl = path,
            name = name,
            author = author,
            originName = fileName,
            coverUrl = FileUtils.getPath(
                App.INSTANCE.externalFilesDir,
                "covers",
                "${MD5Utils.md5Encode16(path)}.jpg"
            )
        )
        App.db.bookDao.insert(book)
        return book
    }

    fun deleteBook(book: Book, deleteOriginal: Boolean) {
        kotlin.runCatching {
            if (book.isLocalTxt()) {
                val bookFile = FileUtils.getFile(cacheFolder, book.originName)
                bookFile.delete()
            }

            if (deleteOriginal) {
                if (book.bookUrl.isContentScheme()) {
                    val uri = Uri.parse(book.bookUrl)
                    DocumentFile.fromSingleUri(App.INSTANCE, uri)?.delete()
                } else {
                    FileUtils.deleteFile(book.bookUrl)
                }
            }
        }
    }
}