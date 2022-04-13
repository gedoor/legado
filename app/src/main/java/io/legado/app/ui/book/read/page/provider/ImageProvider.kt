package io.legado.app.ui.book.read.page.provider

import android.graphics.Bitmap
import android.util.Size
import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.help.BookHelp
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.glide.ImageLoader
import io.legado.app.model.localBook.EpubFile
import io.legado.app.utils.BitmapUtils
import io.legado.app.utils.FileUtils
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import splitties.init.appCtx
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

object ImageProvider {

    private val errorBitmap: Bitmap? by lazy {
        BitmapUtils.decodeBitmap(
            appCtx,
            R.drawable.image_loading_error,
            ChapterProvider.visibleWidth,
            ChapterProvider.visibleHeight
        )
    }

    private suspend fun cacheImage(
        book: Book,
        src: String,
        bookSource: BookSource?
    ): File {
        val vFile = BookHelp.getImage(book, src)
        if (!vFile.exists()) {
            if (book.isEpub()) {
                EpubFile.getImage(book, src)?.use { input ->
                    val newFile = FileUtils.createFileIfNotExist(vFile.absolutePath)
                    @Suppress("BlockingMethodInNonBlockingContext")
                    FileOutputStream(newFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } else {
                BookHelp.saveImage(bookSource, book, src)
            }
        }
        return vFile
    }

    suspend fun getImageSize(
        book: Book,
        src: String,
        bookSource: BookSource?
    ): Size {
        val file = cacheImage(book, src, bookSource)
        return suspendCancellableCoroutine { block ->
            kotlin.runCatching {
                ImageLoader.loadBitmap(appCtx, file.absolutePath).submit()
                    .getSize { width, height ->
                        block.resume(Size(width, height))
                    }
            }.onFailure {
                block.cancel(it)
            }
        }
    }

    fun getImage(
        book: Book,
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
            Coroutine.async { vFile.delete() }
            //must call this method on a background thread
            //ImageLoader.loadBitmap(appCtx, R.drawable.image_loading_error).submit().get()
            errorBitmap
        }
    }

}
