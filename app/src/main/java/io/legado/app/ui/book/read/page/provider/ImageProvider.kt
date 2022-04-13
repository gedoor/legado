package io.legado.app.ui.book.read.page.provider

import android.graphics.Bitmap
import io.legado.app.R
import io.legado.app.constant.AppLog.putDebug
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.help.BookHelp
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.glide.ImageLoader
import io.legado.app.model.localBook.EpubFile
import io.legado.app.utils.FileUtils
import io.legado.app.utils.BitmapUtils
import io.legado.app.utils.isXml
import kotlinx.coroutines.runBlocking
import splitties.init.appCtx
import java.io.File
import java.io.FileOutputStream

object ImageProvider {

    private val errorBitmap: Bitmap? by lazy {
        BitmapUtils.decodeBitmap(
           appCtx,
           R.drawable.image_loading_error,
           ChapterProvider.visibleWidth,
           ChapterProvider.visibleHeight
        )
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
            ImageLoader.loadBitmap(appCtx, vFile.absolutePath)
                .submit(ChapterProvider.visibleWidth,ChapterProvider.visibleHeight)
                .get()
       } catch (e: Exception) {
           Coroutine.async {
               putDebug("${vFile.absolutePath} 解码失败", e)
               if (FileUtils.readText(vFile.absolutePath).isXml()) {
                   putDebug("${vFile.absolutePath}为xml，自动删除")
                   vFile.delete()
               }
           }
           errorBitmap
       }
    }

}
