package io.legado.app.ui.book.read.page.provider

import android.graphics.Bitmap
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.help.BookHelp
import io.legado.app.help.glide.ImageLoader
import io.legado.app.model.localBook.EpubFile
import io.legado.app.utils.BitmapUtils
import io.legado.app.utils.FileUtils
import kotlinx.coroutines.runBlocking
import splitties.init.appCtx
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

object ImageProvider {

    fun getImage(
        book: Book,
        chapterIndex: Int,
        src: String,
        bookSource: BookSource?,
        onUi: Boolean = false,
    ): Bitmap? {
        val vFile = BookHelp.getImage(book, src)
        if (!vFile.exists()) {
            if (book.isEpub()) {
                EpubFile.getImage(book, src)?.use { input ->
                    val newFile = FileUtils.createFileIfNotExist(vFile.absolutePath)
                    FileOutputStream(newFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } else if (!onUi) {
                runBlocking {
                    BookHelp.saveImage(bookSource, book, src)
                }
            }
        }
        return try {
            ImageLoader.loadBitmap(appCtx, vFile.absolutePath).submit().get()
        } catch (e: Exception) {
            null
        }
    }

}
