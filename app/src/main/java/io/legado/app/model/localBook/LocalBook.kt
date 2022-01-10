package io.legado.app.model.localBook

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.R
import io.legado.app.constant.AppConst
import io.legado.app.constant.AppPattern
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.AppConfig
import io.legado.app.help.BookHelp
import io.legado.app.model.TocEmptyException
import io.legado.app.utils.*
import splitties.init.appCtx
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.script.SimpleBindings

object LocalBook {

    private const val folderName = "bookTxt"
    val cacheFolder: File by lazy {
        FileUtils.createFolderIfNotExist(appCtx.externalFiles, folderName)
    }

    @Throws(FileNotFoundException::class, SecurityException::class)
    fun getBookInputStream(book: Book): InputStream {
        val uri = Uri.parse(book.bookUrl)
        if (uri.isContentScheme()) {
            return appCtx.contentResolver.openInputStream(uri)!!
        }
        val file = File(uri.path!!)
        if (file.exists()) {
            return FileInputStream(File(uri.path!!))
        }
        throw FileNotFoundException("${uri.path} 文件不存在")
    }

    @Throws(Exception::class)
    fun getChapterList(book: Book): ArrayList<BookChapter> {
        val chapters = when {
            book.isEpub() -> {
                EpubFile.getChapterList(book)
            }
            book.isUmd() -> {
                UmdFile.getChapterList(book)
            }
            else -> {
                TextFile.getChapterList(book)
            }
        }
        if (chapters.isEmpty()) {
            throw TocEmptyException(appCtx.getString(R.string.chapter_list_empty))
        }
        return chapters
    }

    fun getContent(book: Book, chapter: BookChapter): String? {
        return try {
            when {
                book.isEpub() -> {
                    EpubFile.getContent(book, chapter)
                }
                book.isUmd() -> {
                    UmdFile.getContent(book, chapter)
                }
                else -> {
                    TextFile.getContent(book, chapter)
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
            e.localizedMessage
        }
    }

    fun importFile(uri: Uri): Book {
        val bookUrl: String
        val updateTime: Long
        //这个变量不要修改,否则会导致读取不到缓存
        val fileName = (if (uri.isContentScheme()) {
            bookUrl = uri.toString()
            val doc = DocumentFile.fromSingleUri(appCtx, uri)!!
            updateTime = doc.lastModified()
            doc.name!!
        } else {
            bookUrl = uri.path!!
            val file = File(bookUrl)
            updateTime = file.lastModified()
            file.name
        })
        var book = appDb.bookDao.getBook(bookUrl)
        if (book == null) {
            val nameAuthor = analyzeNameAuthor(fileName)
            book = Book(
                bookUrl = bookUrl,
                name = nameAuthor.first,
                author = nameAuthor.second,
                originName = fileName,
                coverUrl = FileUtils.getPath(
                    appCtx.externalFiles,
                    "covers",
                    "${MD5Utils.md5Encode16(bookUrl)}.jpg"
                ),
                latestChapterTime = updateTime
            )
            if (book.isEpub()) EpubFile.upBookInfo(book)
            if (book.isUmd()) UmdFile.upBookInfo(book)
            appDb.bookDao.insert(book)
        } else {
            //已有书籍说明是更新,删除原有目录
            appDb.bookChapterDao.delByBook(bookUrl)
        }
        return book
    }

    fun analyzeNameAuthor(fileName: String): Pair<String, String> {
        val tempFileName = fileName.substringBeforeLast(".")
        var name: String
        var author: String
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
                try {
                    //在脚本中定义如何分解文件名成书名、作者名
                    val jsonStr = AppConst.SCRIPT_ENGINE.eval(
                        //在用户脚本后添加捕获author、name的代码，只要脚本中author、name有值就会被捕获
                        AppConfig.bookImportFileName + "\nJSON.stringify({author:author,name:name})",
                        //将文件名注入到脚步的src变量中
                        SimpleBindings().also { it["src"] = tempFileName }
                    ).toString()
                    val bookMess =
                        GSON.fromJsonObject<HashMap<String, String>>(jsonStr) ?: HashMap()
                    name = bookMess["name"] ?: tempFileName
                    author = bookMess["author"]?.takeIf { it.length != tempFileName.length } ?: ""
                } catch (e: Exception) {
                    name = tempFileName.replace(AppPattern.nameRegex, "")
                    author = tempFileName.replace(AppPattern.authorRegex, "")
                        .takeIf { it.length != tempFileName.length } ?: ""
                }
            } else {
                name = tempFileName.replace(AppPattern.nameRegex, "")
                author = tempFileName.replace(AppPattern.authorRegex, "")
                    .takeIf { it.length != tempFileName.length } ?: ""
            }

        }
        return Pair(name, author)
    }

    fun deleteBook(book: Book, deleteOriginal: Boolean) {
        kotlin.runCatching {
            if (book.isLocalTxt() || book.isUmd()) {
                cacheFolder.getFile(book.originName).delete()
            }
            if (book.isEpub()) {
                FileUtils.delete(
                    cacheFolder.getFile(book.getFolderName())
                )
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
