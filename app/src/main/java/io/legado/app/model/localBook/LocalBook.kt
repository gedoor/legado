package io.legado.app.model.localBook

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.constant.AppConst
import io.legado.app.constant.AppPattern
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.AppConfig
import io.legado.app.help.BookHelp
import io.legado.app.utils.*
import splitties.init.appCtx
import java.io.File
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.script.SimpleBindings

object LocalBook {
    private const val folderName = "bookTxt"
    val cacheFolder: File by lazy {
        val rootFile = appCtx.getExternalFilesDir(null)
            ?: appCtx.externalCacheDir
            ?: appCtx.cacheDir
        FileUtils.createFolderIfNotExist(rootFile, folderName)
    }

    fun getChapterList(book: Book): ArrayList<BookChapter> {
        return when {
            book.isEpub() -> {
                EpubFile.getChapterList(book)
            }
            book.isUmd() -> {
                UmdFile.getChapterList(book)
            }
            else -> {
                AnalyzeTxtFile().analyze(book)
            }
        }
    }

    fun getContext(book: Book, chapter: BookChapter): String? {
        return when {
            book.isEpub() -> {
                EpubFile.getContent(book, chapter)
            }
            book.isUmd() -> {
                UmdFile.getContent(book, chapter)
            }
            else -> {
                AnalyzeTxtFile.getContent(book, chapter)
            }
        }
    }

    fun importFile(uri: Uri): Book {
        val path: String
        //这个变量不要修改,否则会导致读取不到缓存
        val fileName = (if (uri.isContentScheme()) {
            path = uri.toString()
            val doc = DocumentFile.fromSingleUri(appCtx, uri)
            doc?.let {
                val bookFile = FileUtils.getFile(cacheFolder, it.name!!)
                if (!bookFile.exists()) {
                    bookFile.createNewFile()
                    doc.readBytes(appCtx)?.let { bytes ->
                        bookFile.writeBytes(bytes)
                    }
                }
            }
            doc?.name!!
        } else {
            path = uri.path!!
            File(path).name
        })
        val tempFileName = fileName.replace(Regex("\\.txt$"), "")

        val name: String
        val author: String

        //匹配(知轩藏书常用格式) 《书名》其它信息作者：作者名.txt
        val m1 = Pattern
            .compile("(.*?)《([^《》]+)》(.*)")
            .matcher(tempFileName)

        //匹配 书名 by 作者名.txt
        val m2 = Pattern
            .compile("(^)(.+) by (.+)$")
            .matcher(tempFileName)

        (m1.takeIf { m1.find() } ?: m2.takeIf { m2.find() }).run {

            if (this is Matcher) {

                //按默认格式将文件名分解成书名、作者名
                name = group(2)!!
                author = BookHelp.formatBookAuthor((group(1) ?: "") + (group(3) ?: ""))

            } else if (!AppConfig.bookImportFileName.isNullOrBlank()) {

                //在脚本中定义如何分解文件名成书名、作者名
                val jsonStr = AppConst.SCRIPT_ENGINE.eval(

                    //在用户脚本后添加捕获author、name的代码，只要脚本中author、name有值就会被捕获
                    AppConfig.bookImportFileName + "\nJSON.stringify({author:author,name:name})",

                    //将文件名注入到脚步的src变量中
                    SimpleBindings().also { it["src"] = tempFileName }
                ).toString()
                val bookMess = GSON.fromJsonObject<HashMap<String, String>>(jsonStr) ?: HashMap()
                name = bookMess["name"] ?: tempFileName
                author = bookMess["author"]?.takeIf { it.length != tempFileName.length } ?: ""

            } else {

                name = tempFileName.replace(AppPattern.nameRegex, "")
                author = tempFileName.replace(AppPattern.authorRegex, "")
                    .takeIf { it.length != tempFileName.length } ?: ""

            }

        }

        val book = Book(
            bookUrl = path,
            name = name,
            author = author,
            originName = fileName,
            coverUrl = FileUtils.getPath(
                appCtx.externalFiles,
                "covers",
                "${MD5Utils.md5Encode16(path)}.jpg"
            )
        )
        if (book.isEpub()) EpubFile.upBookInfo(book)
        if (book.isUmd()) UmdFile.upBookInfo(book)
        appDb.bookDao.insert(book)
        return book
    }

    fun deleteBook(book: Book, deleteOriginal: Boolean) {
        kotlin.runCatching {
            if (book.isLocalTxt() || book.isUmd()) {
                val bookFile = FileUtils.getFile(cacheFolder, book.originName)
                bookFile.delete()
            }
            if (book.isEpub()) {
                val bookFile = BookHelp.getEpubFile(book).parentFile
                if (bookFile != null && bookFile.exists()) {
                    FileUtils.delete(bookFile, true)
                }

            }

            if (deleteOriginal) {
                if (book.bookUrl.isContentScheme()) {
                    val uri = Uri.parse(book.bookUrl)
                    DocumentFile.fromSingleUri(appCtx, uri)?.delete()
                } else {
                    FileUtils.deleteFile(book.bookUrl)
                }
            }
        }
    }
}
