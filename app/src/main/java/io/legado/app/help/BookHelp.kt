package io.legado.app.help

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.SearchBook

object BookHelp {

    fun toBook(searchBook: SearchBook): Book {
        val book = Book()
        book.name = searchBook.name
        book.author = searchBook.author
        book.kind = searchBook.kind
        book.bookUrl = searchBook.bookUrl
        book.origin = searchBook.origin
        book.wordCount = searchBook.wordCount
        book.latestChapterTitle = searchBook.latestChapterTitle
        book.coverUrl = searchBook.coverUrl

        return book
    }


    fun formatAuthor(author: String?): String {
        return author
            ?.replace("作\\s*者[\\s:：]*".toRegex(), "")
            ?.replace("\\s+".toRegex(), " ")
            ?.trim { it <= ' ' }
            ?: ""
    }

}