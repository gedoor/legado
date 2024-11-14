package io.legado.app.utils

import io.legado.app.data.entities.BookChapter

fun BookChapter.internString() {
    title = title.intern()
    bookUrl = bookUrl.intern()
}
