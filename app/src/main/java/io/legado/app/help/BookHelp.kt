package io.legado.app.help

import io.legado.app.App
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.utils.getPrefString
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

object BookHelp {

    var downloadPath = App.INSTANCE.getPrefString("downloadPath") ?: App.INSTANCE.getExternalFilesDir(null)

    fun saveContent(book: Book, bookChapter: BookChapter, content: String) {
        if (content.isEmpty()) {
            return
        }
        val filePath = getChapterPath(book, bookChapter)
        val file = FileHelp.getFile(filePath)
        //获取流并存储
        try {
            BufferedWriter(FileWriter(file)).use { writer ->
                writer.write(bookChapter.title + "\n\n")
                writer.write(content)
                writer.write("\n\n")
                writer.flush()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    fun hasContent(book: Book, bookChapter: BookChapter): Boolean {


        return false
    }

    fun getContent(book: Book, bookChapter: BookChapter): String? {


        return null
    }

    private fun getChapterPath(book: Book, bookChapter: BookChapter): String {
        val bookFolder = formatFolderName(book.name + book.bookUrl)
        val chapterFile = String.format("%05d-%s", bookChapter.index, formatFolderName(bookChapter.title))
        return "$downloadPath${File.separator}book_cache${File.separator}$bookFolder${File.separator}$chapterFile.nb"
    }

    private fun formatFolderName(folderName: String): String {
        return folderName.replace("/", "")
            .replace(":", "")
            .replace(".", "")
    }

    fun formatAuthor(author: String?): String {
        return author
            ?.replace("作\\s*者[\\s:：]*".toRegex(), "")
            ?.replace("\\s+".toRegex(), " ")
            ?.trim { it <= ' ' }
            ?: ""
    }

}