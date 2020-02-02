package io.legado.app.model.localBook

import android.content.Context
import android.net.Uri
import io.legado.app.App
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.TxtTocRule
import io.legado.app.utils.*
import java.io.File
import java.io.RandomAccessFile

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
        val toc = arrayListOf<BookChapter>()
        //获取文件流
        val bookStream = RandomAccessFile(bookFile, "r")
        val tocRule = getTocRule(bookStream)


    }


    private fun getTocRule(bookStream: RandomAccessFile): String? {
        val tocRules = getTocRules()
        var tocRule: String? = null
        //首先获取128k的数据
        val buffer = ByteArray(10240)
        val length = bookStream.read(buffer, 0, buffer.size)
        for (str in tocRules) {

        }
        return tocRule
    }

    private fun getTocRules(): List<TxtTocRule> {
        val rules = App.db.txtTocRule().all
        if (rules.isEmpty()) {
            App.INSTANCE.assets.open("txtTocRule.json").readBytes().let { byteArray ->
                GSON.fromJsonArray<TxtTocRule>(String(byteArray))?.let {
                    App.db.txtTocRule().insert(*it.toTypedArray())
                    return it
                }
            }
        }
        return rules
    }
}