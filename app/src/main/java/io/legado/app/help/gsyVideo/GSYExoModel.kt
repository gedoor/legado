package io.legado.app.help.gsyVideo

import com.shuyu.gsyvideoplayer.model.GSYModel
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import java.io.File

class GSYExoModel(
    urls: List<BookChapter>,
    book: Book?,
    source : BookSource?,
    mapHeadData: MutableMap<String?, String?>?,
    index: Int,
    loop: Boolean,
    speed: Float,
    cache: Boolean,
    cachePath: File?,
    overrideExtension: String?
) : GSYModel("", mapHeadData, loop, speed, cache, cachePath, overrideExtension) {
    var urls: List<BookChapter>? = null
    var index: Int = 0
    var book: Book? = null
    var source: BookSource? = null

    init {
        this.urls = urls
        this.index = index
        this.book = book
        this.source = source
    }
}