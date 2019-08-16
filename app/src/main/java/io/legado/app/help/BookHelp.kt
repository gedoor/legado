package io.legado.app.help

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter

object BookHelp {

    fun saveContent(book: Book, bookChapter: BookChapter, content: String) {

    }

    fun hasContent(book: Book, bookChapter: BookChapter): Boolean {


        return false
    }

    fun getContent(book: Book, bookChapter: BookChapter): String? {


        return null
    }

    fun formatAuthor(author: String?): String {
        return author
            ?.replace("作\\s*者[\\s:：]*".toRegex(), "")
            ?.replace("\\s+".toRegex(), " ")
            ?.trim { it <= ' ' }
            ?: ""
    }

}