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

    private val cache = hashMapOf<Int, HashMap<String, Bitmap>>()

    @Synchronized
    fun getCache(chapterIndex: Int, src: String): Bitmap? {
        return cache[chapterIndex]?.get(src)
    }

    @Synchronized
    fun setCache(chapterIndex: Int, src: String, bitmap: Bitmap) {
        var indexCache = cache[chapterIndex]
        if (indexCache == null) {
            indexCache = hashMapOf()
            cache[chapterIndex] = indexCache
        }
        indexCache[src] = bitmap
    }

    fun getImage(book: Book, chapterIndex: Int, src: String): Bitmap? {
        getCache(chapterIndex, src)?.let {
            return it
        }
        val vFile = FileUtils.getFile(
            App.INSTANCE.externalFilesDir,
            "${MD5Utils.md5Encode16(src)}${src.substringAfterLast(".")}",
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
                HttpHelper.getBytes(src, src)?.let {
                    FileUtils.createFileIfNotExist(vFile.absolutePath).writeBytes(it)
                }
            }
        }
        return try {
            val bitmap = BitmapUtils.decodeBitmap(
                vFile.absolutePath,
                ChapterProvider.visibleWidth,
                ChapterProvider.visibleHeight
            )
            setCache(chapterIndex, src, bitmap)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    fun clearAllCache() {
        cache.forEach {indexCache->
            indexCache.value.forEach {
                it.value.recycle()
            }
        }
        cache.clear()
    }

    fun clearOut(chapterIndex: Int) {
        cache.forEach {indexCache->
            if (indexCache.key !in chapterIndex - 1..chapterIndex + 1) {
                indexCache.value.forEach {
                    it.value.recycle()
                }
                cache.remove(indexCache.key)
            }
        }
    }

}