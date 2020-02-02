package io.legado.app.model.localBook

import android.content.Context
import android.net.Uri
import io.legado.app.App
import io.legado.app.data.entities.Book
import io.legado.app.utils.DocumentUtils
import io.legado.app.utils.EncodingDetect
import io.legado.app.utils.FileUtils
import java.io.File

object AnalyzeTxtFile {
    private const val folderName = "bookTxt"
    private val cacheFolder: File by lazy {
        val rootFile = App.INSTANCE.getExternalFilesDir(null)
            ?: App.INSTANCE.externalCacheDir
            ?: App.INSTANCE.cacheDir
        FileUtils.createFileIfNotExist(rootFile, subDirs = *arrayOf(folderName))
    }

    fun analyze(context: Context, book: Book) {
        val uri = Uri.parse(book.bookUrl)
        val bookFile = FileUtils.getFile(cacheFolder, book.originName, subDirs = *arrayOf())
        if (!bookFile.exists()) {
            bookFile.createNewFile()
            DocumentUtils.readBytes(context, uri)?.let {
                bookFile.writeBytes(it)
            }
        }
        book.charset = EncodingDetect.getEncode(bookFile)

    }

}