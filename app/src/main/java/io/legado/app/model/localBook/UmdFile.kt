package io.legado.app.model.localBook

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.utils.*
import me.ag2s.umdlib.domain.UmdBook
import me.ag2s.umdlib.umd.UmdReader
import java.io.File
import java.io.InputStream

class UmdFile(var book: Book) {
    companion object : BaseLocalBookParse {
        private var uFile: UmdFile? = null

        @Synchronized
        private fun getUFile(book: Book): UmdFile {
            if (uFile == null || uFile?.book?.bookUrl != book.bookUrl) {
                uFile = UmdFile(book)
                return uFile!!
            }
            uFile?.book = book
            return uFile!!
        }

        @Synchronized
        override fun getChapterList(book: Book): ArrayList<BookChapter> {
            return getUFile(book).getChapterList()
        }

        @Synchronized
        override fun getContent(book: Book, chapter: BookChapter): String? {
            return getUFile(book).getContent(chapter)
        }

        @Synchronized
        override fun getImage(
            book: Book,
            href: String
        ): InputStream? {
            return getUFile(book).getImage(href)
        }


        @Synchronized
        override fun upBookInfo(book: Book) {
            return getUFile(book).upBookInfo()
        }
    }


    private var umdBook: UmdBook? = null
        get() {
            if (field != null) {
                return field
            }
            field = readUmd()
            return field
        }


    init {
        try {
            umdBook?.let {
                if (book.coverUrl.isNullOrEmpty()) {
                    book.coverUrl = LocalBook.getCoverPath(book)
                }
                if (!File(book.coverUrl!!).exists()) {
                    FileUtils.writeBytes(book.coverUrl!!, it.cover.coverData)
                }
            }
        } catch (e: Exception) {
            e.printOnDebug()
        }
    }

    private fun readUmd(): UmdBook? {
        val input = LocalBook.getBookInputStream(book)
        return UmdReader().read(input)
    }

    private fun upBookInfo() {
        if (umdBook == null) {
            uFile = null
            book.intro = "书籍导入异常"
        } else {
            val hd = umdBook!!.header
            book.name = hd.title
            book.author = hd.author
            book.kind = hd.bookType
        }
    }

    private fun getContent(chapter: BookChapter): String? {
        return umdBook?.chapters?.getContentString(chapter.index)
    }

    private fun getChapterList(): ArrayList<BookChapter> {
        val chapterList = ArrayList<BookChapter>()
        umdBook?.chapters?.titles?.forEachIndexed { index, _ ->
            val title = umdBook!!.chapters.getTitle(index)
            val chapter = BookChapter()
            chapter.title = title
            chapter.index = index
            chapter.bookUrl = book.bookUrl
            chapter.url = index.toString()
            DebugLog.d(javaClass.name, chapter.url)
            chapterList.add(chapter)
        }
        return chapterList
    }

    private fun getImage(@Suppress("UNUSED_PARAMETER") href: String): InputStream? {
        return null
    }

}