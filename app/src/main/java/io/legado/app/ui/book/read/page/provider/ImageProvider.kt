package io.legado.app.ui.book.read.page.provider

import android.graphics.Bitmap
import io.legado.app.App
import io.legado.app.data.entities.Book
import io.legado.app.help.http.HttpHelper
import io.legado.app.model.localBook.EPUBFile
import io.legado.app.utils.BitmapUtils
import io.legado.app.utils.FileUtils
import io.legado.app.utils.MD5Utils
import io.legado.app.utils.externalFilesDir
import java.io.FileOutputStream

object ImageProvider {


    fun getImage(book: Book, src: String): Bitmap? {
        val vFile = FileUtils.getFile(
            App.INSTANCE.externalFilesDir,
            "${MD5Utils.md5Encode16(src)}.jpg",
            "images", book.name
        )
        if (!vFile.exists()) {
            if (book.isEpub()) {
                EPUBFile.getImage(book, src).use {
                    val out = FileOutputStream(FileUtils.createFileIfNotExist(vFile.absolutePath))
                    it?.copyTo(out)
                    out.flush()
                    out.close()
                }
            } else {
                HttpHelper.getBytes(src)?.let {
                    FileUtils.createFileIfNotExist(vFile.absolutePath).writeBytes(it)
                }
            }
        }
        return try {
            BitmapUtils.decodeBitmap(
                vFile.absolutePath,
                ChapterProvider.visibleWidth,
                ChapterProvider.visibleHeight
            )
        } catch (e: Exception) {
            null
        }
    }

}