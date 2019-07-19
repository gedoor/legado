package io.legado.app.model.webbook

import io.legado.app.data.entities.BookChapter

data class ChapterData(
    var chapterList: List<BookChapter>,
    var nextUrlList: List<String>
)