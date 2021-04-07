package io.legado.app.model.localBook

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.BookHelp
import io.legado.app.utils.*
import splitties.init.appCtx
import java.io.File
import java.util.regex.Pattern


object LocalBook {
    private const val folderName = "bookTxt"
    val cacheFolder: File by lazy {
        val rootFile = appCtx.getExternalFilesDir(null)
            ?: appCtx.externalCacheDir
            ?: appCtx.cacheDir
        FileUtils.createFolderIfNotExist(rootFile, folderName)
    }

    fun getChapterList(book: Book): ArrayList<BookChapter> {
        return if (book.isEpub()) {
            EpubFile.getChapterList(book)
        } else {
            AnalyzeTxtFile().analyze(book)
        }
    }

    fun getContext(book: Book, chapter: BookChapter): String? {
        return if (book.isEpub()) {
            EpubFile.getContent(book, chapter)
        } else {
            AnalyzeTxtFile.getContent(book, chapter)
        }
    }

    fun importFile(uri: Uri): Book {
        val path: String
        val fileName = if (uri.isContentScheme()) {
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
        }

        val name: String
        val author: String

        if (("《" in fileName && "》" in fileName)
            || "作者" in fileName
            || (fileName.contains(" by ", true))
        ) {


            //匹配(知轩藏书常用格式) 《书名》其它信息作者：作者名.txt
            val m1 = Pattern
                .compile("《(.*?)》.*?作者：(.*?)\\.txt")
                .matcher(fileName)
            //匹配 书名 by 作者名.txt
            val m2 = Pattern
                .compile("txt\\.(.*?) yb (.*?)$")
                .matcher(fileName.reversed())

            if (m1.find()) {
                name = m1.group(1) ?: fileName.replace(".txt", "")
                author = m1.group(2) ?: ""
                BookHelp.formatBookAuthor(author)
            } else if (m2.find()) {
                var temp = m2.group(2)
                name = if (temp==null||temp == "") {
                    fileName.replace(".txt", "")
                } else {
                    temp.reversed()
                }
                temp = m2.group(1) ?: ""
                author = temp.reversed()
                BookHelp.formatBookAuthor(author)
            } else {

                val st = fileName.indexOf("《")
                val e = fileName.indexOf("》")
                name = if (e > st && st != -1) {
                    fileName.substring(st + 1, e)
                } else {
                    fileName.replace(".txt", "")
                }


                val s = fileName.indexOf("作者")
                author = if (s != -1 && s + 2 < fileName.length) {
                    fileName.substring(s + 2).replace(".txt", "")
                } else {
                    ""
                }
                BookHelp.formatBookAuthor(author)

            }
        } else {

            name = fileName.replace(".txt", "")
            author = ""
        }


        val book = Book(
            bookUrl = path,
            name = name,
            author = author,
            originName = fileName,
            coverUrl = FileUtils.getPath(
                appCtx.externalFilesDir,
                "covers",
                "${MD5Utils.md5Encode16(path)}.jpg"
            )
        )
        if (book.isEpub()) EpubFile.upBookInfo(book)
        appDb.bookDao.insert(book)
        return book
    }

    fun deleteBook(book: Book, deleteOriginal: Boolean) {
        kotlin.runCatching {
            if (book.isLocalTxt()) {
                val bookFile = FileUtils.getFile(cacheFolder, book.originName)
                bookFile.delete()
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