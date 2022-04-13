package io.legado.app.ui.book.read.page.provider

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Size
import io.legado.app.R
import io.legado.app.constant.AppLog.putDebug
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.help.BookHelp
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.glide.ImageLoader
import io.legado.app.model.localBook.EpubFile
import io.legado.app.utils.FileUtils
import io.legado.app.utils.isXml
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import splitties.init.appCtx
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

object ImageProvider {

    private val errorBitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(appCtx.resources, R.drawable.image_loading_error)
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
                block.resume(Size(errorBitmap.width, errorBitmap.height))
            }
        }
    }

    fun getImage(
        book: Book,
        src: String,
        bookSource: BookSource?,
        width: Int,
        height: Int
    ): Bitmap? {
        val vFile = runBlocking {
            cacheImage(book, src, bookSource)
        }
        return try {
            ImageLoader.loadBitmap(appCtx, vFile.absolutePath)
                .submit(width, height)
                .get()
        } catch (e: Exception) {
            Coroutine.async {
                putDebug("${vFile.absolutePath} 解码失败\n${e.toString()}", e)
                if (FileUtils.readText(vFile.absolutePath).isXml()) {
                    putDebug("${vFile.absolutePath}为xml，自动删除")
                    vFile.delete()
                }
            }
            errorBitmap
        }
    }

    fun getImage(
        book: Book,
        src: String,
        bookSource: BookSource?
    ): Bitmap? {
        val vFile = runBlocking {
            cacheImage(book, src, bookSource)
        }
        return try {
            ImageLoader.loadBitmap(appCtx, vFile.absolutePath)
                .submit(ChapterProvider.visibleWidth, ChapterProvider.visibleHeight)
                .get()
        } catch (e: Exception) {
            Coroutine.async {
                putDebug("${vFile.absolutePath} 解码失败\n${e.toString()}", e)
                if (FileUtils.readText(vFile.absolutePath).isXml()) {
                    putDebug("${vFile.absolutePath}为xml，自动删除")
                    vFile.delete()
                }
            }
            errorBitmap
        }
    }

}
