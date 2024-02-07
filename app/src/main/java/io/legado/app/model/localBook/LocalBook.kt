package io.legado.app.model.localBook

import android.net.Uri
import android.util.Base64
import androidx.documentfile.provider.DocumentFile
import com.script.SimpleBindings
import com.script.rhino.RhinoScriptEngine
import io.legado.app.R
import io.legado.app.constant.*
import io.legado.app.data.appDb
import io.legado.app.data.entities.BaseSource
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.exception.EmptyFileException
import io.legado.app.exception.NoBooksDirException
import io.legado.app.exception.NoStackTraceException
import io.legado.app.exception.TocEmptyException
import io.legado.app.help.AppWebDav
import io.legado.app.help.book.*
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.webdav.WebDav
import io.legado.app.lib.webdav.WebDavException
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.utils.*
import kotlinx.coroutines.runBlocking
import org.apache.commons.text.StringEscapeUtils
import splitties.init.appCtx
import java.io.*
import java.util.regex.Pattern

/**
 * 书籍文件导入 目录正文解析
 * 支持在线文件(txt epub umd 压缩文件 本地文件
 */
object LocalBook {

    private val nameAuthorPatterns = arrayOf(
        Pattern.compile("(.*?)《([^《》]+)》.*?作者：(.*)"),
        Pattern.compile("(.*?)《([^《》]+)》(.*)"),
        Pattern.compile("(^)(.+) 作者：(.+)$"),
        Pattern.compile("(^)(.+) by (.+)$")
    )

    @Throws(FileNotFoundException::class, SecurityException::class)
    fun getBookInputStream(book: Book): InputStream {
        val uri = book.getLocalUri()
        val inputStream = uri.inputStream(appCtx).getOrNull()
            ?: let {
                book.removeLocalUriCache()
                val localArchiveUri = book.getArchiveUri()
                val webDavUrl = book.getRemoteUrl()
                if (localArchiveUri != null) {
                    // 重新导入对应的压缩包
                    importArchiveFile(localArchiveUri, book.originName) {
                        it.contains(book.originName)
                    }.firstOrNull()?.let {
                        getBookInputStream(it)
                    }
                } else if (webDavUrl != null && downloadRemoteBook(book)) {
                    // 下载远程链接
                    getBookInputStream(book)
                } else {
                    null
                }
            }
        if (inputStream != null) return inputStream
        book.removeLocalUriCache()
        throw FileNotFoundException("${uri.path} 文件不存在")
    }

    fun getLastModified(book: Book): Result<Long> {
        return kotlin.runCatching {
            val uri = Uri.parse(book.bookUrl)
            if (uri.isContentScheme()) {
                return@runCatching DocumentFile.fromSingleUri(appCtx, uri)!!.lastModified()
            }
            val file = File(uri.path!!)
            if (file.exists()) {
                return@runCatching file.lastModified()
            }
            throw FileNotFoundException("${uri.path} 文件不存在")
        }
    }

    @Throws(TocEmptyException::class)
    fun getChapterList(book: Book): ArrayList<BookChapter> {
        val chapters = when {
            book.isEpub -> {
                EpubFile.getChapterList(book)
            }

            book.isUmd -> {
                UmdFile.getChapterList(book)
            }

            book.isPdf -> {
                PdfFile.getChapterList(book)
            }

            else -> {
                TextFile.getChapterList(book)
            }
        }
        if (chapters.isEmpty()) {
            throw TocEmptyException(appCtx.getString(R.string.chapter_list_empty))
        }
        val list = ArrayList(LinkedHashSet(chapters))
        list.forEachIndexed { index, bookChapter -> bookChapter.index = index }
        book.latestChapterTitle = list.last().title
        book.totalChapterNum = list.size
        book.save()
        return list
    }

    fun getContent(book: Book, chapter: BookChapter): String? {
        var content = try {
            when {
                book.isEpub -> {
                    EpubFile.getContent(book, chapter)
                }

                book.isUmd -> {
                    UmdFile.getContent(book, chapter)
                }

                book.isPdf -> {
                    PdfFile.getContent(book, chapter)
                }

                else -> {
                    TextFile.getContent(book, chapter)
                }
            }
        } catch (e: Exception) {
            e.printOnDebug()
            AppLog.put("获取本地书籍内容失败\n${e.localizedMessage}", e)
            "获取本地书籍内容失败\n${e.localizedMessage}"
        }
        if (book.isEpub) {
            content ?: return null
            if (content.indexOf('&') > -1) {
                content = content.replace("&lt;img", "&lt; img", true)
                return StringEscapeUtils.unescapeHtml4(content)
            }
        }
        return content
    }

    fun getCoverPath(book: Book): String {
        return getCoverPath(book.bookUrl)
    }

    private fun getCoverPath(bookUrl: String): String {
        return FileUtils.getPath(
            appCtx.externalFiles,
            "covers",
            "${MD5Utils.md5Encode16(bookUrl)}.jpg"
        )
    }

    /**
     * 下载在线的文件并自动导入到阅读（txt umd epub)
     */
    fun importFileOnLine(
        str: String,
        fileName: String,
        source: BaseSource? = null,
    ): Book {
        return importFile(saveBookFile(str, fileName, source))
    }

    /**
     * 导入本地文件
     */
    fun importFile(uri: Uri): Book {
        val bookUrl: String
        //updateTime变量不要修改,否则会导致读取不到缓存
        val (fileName, _, _, updateTime, _) = FileDoc.fromUri(uri, false).apply {
            if (size == 0L) throw EmptyFileException("Unexpected empty File")

            bookUrl = toString()
        }
        var book = appDb.bookDao.getBook(bookUrl)
        if (book == null) {
            val nameAuthor = analyzeNameAuthor(fileName)
            book = Book(
                type = BookType.text or BookType.local,
                bookUrl = bookUrl,
                name = nameAuthor.first,
                author = nameAuthor.second,
                originName = fileName,
                latestChapterTime = updateTime,
                order = appDb.bookDao.minOrder - 1
            )
            if (book.isEpub) EpubFile.upBookInfo(book)
            if (book.isUmd) UmdFile.upBookInfo(book)
            if (book.isPdf) PdfFile.upBookInfo(book)
            appDb.bookDao.insert(book)
        } else {
            //已有书籍说明是更新,删除原有目录
            appDb.bookChapterDao.delByBook(bookUrl)
        }
        return book
    }

    /* 导入压缩包内的书籍 */
    fun importArchiveFile(
        archiveFileUri: Uri,
        saveFileName: String? = null,
        filter: ((String) -> Boolean)? = null
    ): List<Book> {
        val archiveFileDoc = FileDoc.fromUri(archiveFileUri, false)
        val files = ArchiveUtils.deCompress(archiveFileDoc, filter = filter)
        if (files.isEmpty()) throw NoStackTraceException(appCtx.getString(R.string.unsupport_archivefile_entry))
        return files.map {
            saveBookFile(FileInputStream(it), saveFileName ?: it.name).let { uri ->
                importFile(uri).apply {
                    //附加压缩包名称 以便解压文件被删后再解压
                    origin = "${BookType.localTag}::${archiveFileDoc.name}"
                    addType(BookType.archive)
                    save()
                }
            }
        }
    }

    /* 批量导入 支持自动导入压缩包的支持书籍 */
    fun importFiles(uri: Uri): List<Book> {
        val books = mutableListOf<Book>()
        val fileDoc = FileDoc.fromUri(uri, false)
        if (ArchiveUtils.isArchive(fileDoc.name)) {
            books.addAll(
                importArchiveFile(uri) {
                    it.matches(AppPattern.bookFileRegex)
                }
            )
        } else {
            books.add(importFile(uri))
        }
        return books
    }

    fun importFiles(uris: List<Uri>) {
        var errorCount = 0
        uris.forEach { uri ->
            val fileDoc = FileDoc.fromUri(uri, false)
            kotlin.runCatching {
                if (ArchiveUtils.isArchive(fileDoc.name)) {
                    importArchiveFile(uri) {
                        it.matches(AppPattern.bookFileRegex)
                    }
                } else {
                    importFile(uri)
                }
            }.onFailure {
                AppLog.put("ImportFile Error:\nFile $fileDoc\n${it.localizedMessage}", it)
                errorCount += 1
            }
        }
        if (errorCount == uris.size) throw NoStackTraceException("ImportFiles Error:\nAll input files occur error")
    }

    /**
     * 从文件分析书籍必要信息（书名 作者等）
     */
    private fun analyzeNameAuthor(fileName: String): Pair<String, String> {
        val tempFileName = fileName.substringBeforeLast(".")
        var name = ""
        var author = ""
        if (!AppConfig.bookImportFileName.isNullOrBlank()) {
            try {
                //在用户脚本后添加捕获author、name的代码，只要脚本中author、name有值就会被捕获
                val js =
                    AppConfig.bookImportFileName + "\nJSON.stringify({author:author,name:name})"
                //在脚本中定义如何分解文件名成书名、作者名
                val jsonStr = RhinoScriptEngine.run {
                    val bindings = SimpleBindings()
                    bindings["src"] = tempFileName
                    eval(js, bindings)
                }.toString()
                val bookMess = GSON.fromJsonObject<HashMap<String, String>>(jsonStr)
                    .getOrThrow()
                name = bookMess["name"] ?: ""
                author = bookMess["author"]?.takeIf { it.length != tempFileName.length } ?: ""
            } catch (e: Exception) {
                AppLog.put("执行导入文件名规则出错\n${e.localizedMessage}", e)
            }
        }
        if (name.isBlank()) {
            for (pattern in nameAuthorPatterns) {
                pattern.matcher(tempFileName).takeIf { it.find() }?.run {
                    name = group(2)!!
                    val group1 = group(1) ?: ""
                    val group3 = group(3) ?: ""
                    author = BookHelp.formatBookAuthor(group1 + group3)
                    return Pair(name, author)
                }
            }
            name = BookHelp.formatBookName(tempFileName)
            author = BookHelp.formatBookAuthor(tempFileName.replace(name, ""))
                .takeIf { it.length != tempFileName.length } ?: ""
        }
        return Pair(name, author)
    }

    fun deleteBook(book: Book, deleteOriginal: Boolean) {
        kotlin.runCatching {
            BookHelp.clearCache(book)
            if (deleteOriginal) {
                if (book.bookUrl.isContentScheme()) {
                    val uri = Uri.parse(book.bookUrl)
                    DocumentFile.fromSingleUri(appCtx, uri)?.delete()
                } else {
                    FileUtils.delete(book.bookUrl)
                }
            }
        }
    }

    /**
     * 下载在线的文件
     */
    fun saveBookFile(
        str: String,
        fileName: String,
        source: BaseSource? = null,
    ): Uri {
        AppConfig.defaultBookTreeUri
            ?: throw NoBooksDirException()
        val inputStream = when {
            str.isAbsUrl() -> AnalyzeUrl(str, source = source).getInputStream()
            str.isDataUrl() -> ByteArrayInputStream(
                Base64.decode(
                    str.substringAfter("base64,"),
                    Base64.DEFAULT
                )
            )

            else -> throw NoStackTraceException("在线导入书籍支持http/https/DataURL")
        }
        return saveBookFile(inputStream, fileName)
    }

    fun saveBookFile(
        inputStream: InputStream,
        fileName: String
    ): Uri {
        inputStream.use {
            val defaultBookTreeUri = AppConfig.defaultBookTreeUri
            if (defaultBookTreeUri.isNullOrBlank()) throw NoBooksDirException()
            val treeUri = Uri.parse(defaultBookTreeUri)
            return if (treeUri.isContentScheme()) {
                val treeDoc = DocumentFile.fromTreeUri(appCtx, treeUri)
                var doc = treeDoc!!.findFile(fileName)
                if (doc == null) {
                    doc = treeDoc.createFile(FileUtils.getMimeType(fileName), fileName)
                        ?: throw SecurityException("请重新设置书籍保存位置\nPermission Denial")
                }
                appCtx.contentResolver.openOutputStream(doc.uri)!!.use { oStream ->
                    it.copyTo(oStream)
                }
                doc.uri
            } else {
                val treeFile = File(treeUri.path!!)
                val file = treeFile.getFile(fileName)
                FileOutputStream(file).use { oStream ->
                    it.copyTo(oStream)
                }
                Uri.fromFile(file)
            }
        }
    }

    fun isOnBookShelf(
        fileName: String
    ): Boolean {
        return appDb.bookDao.hasFile(fileName) == true
    }

    //文件类书源 合并在线书籍信息 在线 > 本地
    fun mergeBook(localBook: Book, onLineBook: Book?): Book {
        onLineBook ?: return localBook
        localBook.name = onLineBook.name.ifBlank { localBook.name }
        localBook.author = onLineBook.author.ifBlank { localBook.author }
        localBook.coverUrl = onLineBook.coverUrl
        localBook.intro =
            if (onLineBook.intro.isNullOrBlank()) localBook.intro else onLineBook.intro
        localBook.save()
        return localBook
    }

    //下载book对应的远程文件 并更新Book
    private fun downloadRemoteBook(localBook: Book): Boolean {
        val webDavUrl = localBook.getRemoteUrl()
        if (webDavUrl.isNullOrBlank()) throw NoStackTraceException("Book file is not webDav File")
        try {
            AppConfig.defaultBookTreeUri
                ?: throw NoBooksDirException()
            // 兼容旧版链接
            val webdav: WebDav = kotlin.runCatching {
                WebDav.fromPath(webDavUrl)
            }.getOrElse {
                AppWebDav.authorization?.let { WebDav(webDavUrl, it) }
                    ?: throw WebDavException("Unexpected defaultBookWebDav")
            }
            val inputStream = runBlocking {
                webdav.downloadInputStream()
            }
            inputStream.use {
                if (localBook.isArchive) {
                    // 压缩包
                    val archiveUri = saveBookFile(it, localBook.archiveName)
                    val newBook = importArchiveFile(archiveUri, localBook.originName) { name ->
                        name.contains(localBook.originName)
                    }.first()
                    localBook.origin = newBook.origin
                    localBook.bookUrl = newBook.bookUrl
                } else {
                    // txt epub pdf umd
                    val fileUri = saveBookFile(it, localBook.originName)
                    localBook.bookUrl = FileDoc.fromUri(fileUri, false).toString()
                    localBook.save()
                }
            }
            return true
        } catch (e: Exception) {
            e.printOnDebug()
            AppLog.put("自动下载webDav书籍失败", e)
            return false
        }
    }

}
