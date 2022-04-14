package io.legado.app.ui.book.read.page.provider

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Size
import androidx.collection.LruCache
import io.legado.app.R
import io.legado.app.constant.AppLog.putDebug
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.help.BookHelp
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.localBook.EpubFile
import io.legado.app.utils.BitmapUtils
import io.legado.app.utils.FileUtils
import io.legado.app.utils.isXml
import splitties.init.appCtx
import java.io.File
import java.io.FileOutputStream

object ImageProvider {

    private val errorBitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(appCtx.resources, R.drawable.image_loading_error)
    }

    /**
     *缓存bitmap LruCache实现
     */
    //private val maxMemory = Runtime.getRuntime().maxMemory()
    //private val cacheMemorySize = (maxMemory / 8) as Int
    private val cacheMemorySize: Int = 1024 * 1024 * 1024 //1G
    private val bitmapLruCache = object : LruCache<String, Bitmap>(cacheMemorySize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.getByteCount()
        }
        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldBitmap: Bitmap,
            newBitmap: Bitmap?
        ) {
            if (evicted) {
               oldBitmap.recycle()
               putDebug("自动回收Bitmap path: $key")
               putDebug("bitmapLruCache : ${size()} / ${maxSize()}")
            }
        }
    }

    /**
     *缓存网络图片和epub图片
     */
    suspend fun cacheImage(
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

    /**
     *获取图片宽度高度信息
     */
    suspend fun getImageSize(
        book: Book,
        src: String,
        bookSource: BookSource?
    ): Size {
        val file = cacheImage(book, src, bookSource)
        val op = BitmapFactory.Options()
        // inJustDecodeBounds如果设置为true,仅仅返回图片实际的宽和高,宽和高是赋值给opts.outWidth,opts.outHeight;
        op.inJustDecodeBounds = true
        BitmapFactory.decodeFile(file.absolutePath, op)
        if (op.outWidth == 0 && op.outHeight == 0) {
            return Size(errorBitmap.width, errorBitmap.height)
        }
        return Size(op.outWidth, op.outHeight)
    }

    /**
     *获取bitmap 使用LruCache缓存
     */
    fun getImage(
        book: Book,
        src: String,
        width: Int,
        height: Int? = null
    ): Bitmap {
        val cacheBitmap = bitmapLruCache.get(src)
        if (cacheBitmap != null) return cacheBitmap
        val vFile = BookHelp.getImage(book, src)
        @Suppress("BlockingMethodInNonBlockingContext")
        return try {
            val bitmap = BitmapUtils.decodeBitmap(vFile.absolutePath, width, height)
            bitmapLruCache.put(src, bitmap)
            bitmap
        } catch (e: Exception) {
            Coroutine.async {
                putDebug("${vFile.absolutePath} 解码失败\n$e", e)
                if (FileUtils.readText(vFile.absolutePath).isXml()) {
                    putDebug("${vFile.absolutePath}为xml，自动删除")
                    vFile.delete()
                }
            }
            errorBitmap
        }
    }

}
