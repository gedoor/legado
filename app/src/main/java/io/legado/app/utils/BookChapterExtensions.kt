package io.legado.app.utils

import io.legado.app.data.entities.BookChapter

fun BookChapter.internString() {
    title = title.intern()
    bookUrl = bookUrl.intern()
}

fun List<BookChapter>.chapterRemoveDuplicates(): List<BookChapter> {
    return this.distinctBy { it.url }
        .mapIndexed { i, sChapter ->
            BookChapter(
                sChapter.url,
                sChapter.title,
                sChapter.isVolume,
                sChapter.baseUrl,
                sChapter.bookUrl,
                i,
                sChapter.isVip,
                sChapter.isPay,
                sChapter.resourceUrl,
                sChapter.tag,
                sChapter.wordCount,
                sChapter.start,
                sChapter.end,
                sChapter.startFragmentId, sChapter.endFragmentId, sChapter.variable
            )
        }
}


