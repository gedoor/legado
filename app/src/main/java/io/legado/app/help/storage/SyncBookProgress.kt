package io.legado.app.help.storage

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookProgress
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.lib.webdav.WebDav
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject

@Suppress("BlockingMethodInNonBlockingContext")
object SyncBookProgress {

    private val webDavUrl = "${WebDavHelp.rootWebDavUrl}bookProgress/"

    fun uploadBookProgress(book: Book) {
        Coroutine.async {
            val bookProgress = BookProgress(
                name = book.name,
                author = book.author,
                durChapterIndex = book.durChapterIndex,
                durChapterPos = book.durChapterPos,
                durChapterTime = book.durChapterTime,
                durChapterTitle = book.durChapterTitle
            )
            val json = GSON.toJson(bookProgress)
            val url = getUrl(book)
            if (WebDavHelp.initWebDav()) {
                WebDav(webDavUrl).makeAsDir()
                WebDav(url).upload(json.toByteArray())
            }
        }
    }

    fun getBookProgress(book: Book): BookProgress? {
        if (WebDavHelp.initWebDav()) {
            val url = getUrl(book)
            WebDav(url).download()?.let { byteArray ->
                val json = String(byteArray)
                GSON.fromJsonObject<BookProgress>(json)?.let {
                    return it
                }
            }
        }
        return null
    }

    private fun getUrl(book: Book): String {
        return webDavUrl + book.name + "_" + book.author + ".json"
    }
}