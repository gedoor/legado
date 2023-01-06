package io.legado.app.ui.book.cache

import android.app.Application
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.script.SimpleBindings
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppConst
import io.legado.app.constant.AppLog
import io.legado.app.constant.AppPattern
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.AppWebDav
import io.legado.app.help.book.BookHelp
import io.legado.app.help.book.ContentProcessor
import io.legado.app.help.book.isLocal
import io.legado.app.help.config.AppConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.coroutine.OrderCoroutine
import io.legado.app.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.ag2s.epublib.domain.*
import me.ag2s.epublib.domain.Date
import me.ag2s.epublib.epub.EpubWriter
import me.ag2s.epublib.util.ResourceUtil
import splitties.init.appCtx
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.nio.file.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext


class CacheViewModel(application: Application) : BaseViewModel(application) {
    val upAdapterLiveData = MutableLiveData<String>()
    val exportProgress = ConcurrentHashMap<String, Int>()
    val exportMsg = ConcurrentHashMap<String, String>()
    private val mutex = Mutex()
    val cacheChapters = hashMapOf<String, HashSet<String>>()
    private var loadChapterCoroutine: Coroutine<Unit>? = null

    @Volatile
    private var exportNumber = 0

    fun loadCacheFiles(books: List<Book>) {
        loadChapterCoroutine?.cancel()
        loadChapterCoroutine = execute {
            books.forEach { book ->
                if (!book.isLocal && !cacheChapters.contains(book.bookUrl)) {
                    val chapterCaches = hashSetOf<String>()
                    val cacheNames = BookHelp.getChapterFiles(book)
                    if (cacheNames.isNotEmpty()) {
                        appDb.bookChapterDao.getChapterList(book.bookUrl).forEach { chapter ->
                            if (cacheNames.contains(chapter.getFileName())) {
                                chapterCaches.add(chapter.url)
                            }
                        }
                    }
                    cacheChapters[book.bookUrl] = chapterCaches
                    upAdapterLiveData.sendValue(book.bookUrl)
                }
                ensureActive()
            }
        }
    }

    private fun getExportFileName(book: Book): String {
        val jsStr = AppConfig.bookExportFileName
        if (jsStr.isNullOrBlank()) {
            return "${book.name} 作者：${book.getRealAuthor()}"
        }
        val bindings = SimpleBindings()
        bindings["name"] = book.name
        bindings["author"] = book.getRealAuthor()
        return kotlin.runCatching {
            AppConst.SCRIPT_ENGINE.eval(jsStr, bindings).toString()
        }.onFailure {
            context.toastOnUi("书名规则错误\n${it.localizedMessage}")
        }.getOrDefault("${book.name} 作者：${book.getRealAuthor()}")
    }

    fun export(path: String, book: Book) {
        if (exportProgress.contains(book.bookUrl)) return
        exportProgress[book.bookUrl] = 0
        exportMsg.remove(book.bookUrl)
        upAdapterLiveData.sendValue(book.bookUrl)
        execute {
            mutex.withLock {
                while (exportNumber > 0) {
                    delay(1000)
                }
                exportNumber++
            }
            if (path.isContentScheme()) {
                val uri = Uri.parse(path)
                val doc = DocumentFile.fromTreeUri(context, uri)
                    ?: throw NoStackTraceException("获取导出文档失败")
                export(doc, book)
            } else {
                export(FileUtils.createFolderIfNotExist(path), book)
            }
        }.onError {
            exportProgress.remove(book.bookUrl)
            exportMsg[book.bookUrl] = it.localizedMessage ?: "ERROR"
            upAdapterLiveData.postValue(book.bookUrl)
            AppLog.put("导出书籍<${book.name}>出错", it)
        }.onSuccess {
            exportProgress.remove(book.bookUrl)
            exportMsg[book.bookUrl] = context.getString(R.string.export_success)
            upAdapterLiveData.postValue(book.bookUrl)
        }.onFinally {
            exportNumber--
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun export(doc: DocumentFile, book: Book) {
        val filename = "${getExportFileName(book)}.txt"
        DocumentUtils.delete(doc, filename)
        val bookDoc = DocumentUtils.createFileIfNotExist(doc, filename)
            ?: throw NoStackTraceException("创建文档失败，请尝试重新设置导出文件夹")
        context.contentResolver.openOutputStream(bookDoc.uri, "wa")?.use { bookOs ->
            getAllContents(book) { text, srcList ->
                bookOs.write(text.toByteArray(Charset.forName(AppConfig.exportCharset)))
                srcList?.forEach {
                    val vFile = BookHelp.getImage(book, it.third)
                    if (vFile.exists()) {
                        DocumentUtils.createFileIfNotExist(
                            doc,
                            "${it.second}-${MD5Utils.md5Encode16(it.third)}.jpg",
                            subDirs = arrayOf("${book.name}_${book.author}", "images", it.first)
                        )?.writeBytes(context, vFile.readBytes())
                    }
                }
            }
        }
        if (AppConfig.exportToWebDav) {
            // 导出到webdav
            AppWebDav.exportWebDav(bookDoc.uri, filename)
        }
    }

    private suspend fun export(file: File, book: Book) {
        val filename = "${getExportFileName(book)}.txt"
        val bookPath = FileUtils.getPath(file, filename)
        val bookFile = FileUtils.createFileWithReplace(bookPath)
        getAllContents(book) { text, srcList ->
            bookFile.appendText(text, Charset.forName(AppConfig.exportCharset))
            srcList?.forEach {
                val vFile = BookHelp.getImage(book, it.third)
                if (vFile.exists()) {
                    FileUtils.createFileIfNotExist(
                        file,
                        "${book.name}_${book.author}",
                        "images",
                        it.first,
                        "${it.second}-${MD5Utils.md5Encode16(it.third)}.jpg"
                    ).writeBytes(vFile.readBytes())
                }
            }
        }
        if (AppConfig.exportToWebDav) {
            AppWebDav.exportWebDav(Uri.fromFile(bookFile), filename) // 导出到webdav
        }
    }

    private suspend fun getAllContents(
        book: Book,
        append: (text: String, srcList: ArrayList<Triple<String, Int, String>>?) -> Unit
    ) {
        val useReplace = AppConfig.exportUseReplace && book.getUseReplaceRule()
        val contentProcessor = ContentProcessor.get(book.name, book.origin)
        val qy = "${book.name}\n${
            context.getString(R.string.author_show, book.getRealAuthor())
        }\n${
            context.getString(
                R.string.intro_show,
                "\n" + HtmlFormatter.format(book.getDisplayIntro())
            )
        }"
        append(qy, null)
        if (AppConfig.parallelExportBook) {
            val oc =
                OrderCoroutine<Pair<String, ArrayList<Triple<String, Int, String>>?>>(AppConfig.threadCount)
            appDb.bookChapterDao.getChapterList(book.bookUrl).forEach { chapter ->
                oc.submit { getExportData(book, chapter, contentProcessor, useReplace) }
            }
            oc.collect { index, result ->
                upAdapterLiveData.postValue(book.bookUrl)
                exportProgress[book.bookUrl] = index
                append.invoke(result.first, result.second)
            }
        } else {
            appDb.bookChapterDao.getChapterList(book.bookUrl).forEachIndexed { index, chapter ->
                coroutineContext.ensureActive()
                upAdapterLiveData.postValue(book.bookUrl)
                exportProgress[book.bookUrl] = index
                val result = getExportData(book, chapter, contentProcessor, useReplace)
                append.invoke(result.first, result.second)
            }
        }

    }

    private suspend fun getExportData(
        book: Book,
        chapter: BookChapter,
        contentProcessor: ContentProcessor,
        useReplace: Boolean
    ): Pair<String, ArrayList<Triple<String, Int, String>>?> {
        BookHelp.getContent(book, chapter).let { content ->
            val content1 = contentProcessor
                .getContent(
                    book,
                    chapter,
                    content ?: if (chapter.isVolume) "" else "null",
                    includeTitle = !AppConfig.exportNoChapterName,
                    useReplace = useReplace,
                    chineseConvert = false,
                    reSegment = false
                ).toString()
            if (AppConfig.exportPictureFile) {
                //txt导出图片文件
                val srcList = arrayListOf<Triple<String, Int, String>>()
                content?.split("\n")?.forEachIndexed { index, text ->
                    val matcher = AppPattern.imgPattern.matcher(text)
                    while (matcher.find()) {
                        matcher.group(1)?.let {
                            val src = NetworkUtils.getAbsoluteURL(chapter.url, it)
                            srcList.add(Triple(chapter.title, index, src))
                        }
                    }
                }
                return Pair("\n\n$content1", srcList)
            } else {
                return Pair("\n\n$content1", null)
            }
        }
    }

    //////////////////Start EPUB
    /**
     * 导出Epub
     */
    fun exportEPUB(path: String, book: Book) {
        if (exportProgress.contains(book.bookUrl)) return
        exportProgress[book.bookUrl] = 0
        exportMsg.remove(book.bookUrl)
        upAdapterLiveData.sendValue(book.bookUrl)
        execute {
            mutex.withLock {
                while (exportNumber > 0) {
                    delay(1000)
                }
                exportNumber++
            }
            if (path.isContentScheme()) {
                val uri = Uri.parse(path)
                val doc = DocumentFile.fromTreeUri(context, uri)
                    ?: throw NoStackTraceException("获取导出文档失败")
                exportEpub(doc, book)
            } else {
                exportEpub(FileUtils.createFolderIfNotExist(path), book)
            }
        }.onError {
            exportProgress.remove(book.bookUrl)
            exportMsg[book.bookUrl] = it.localizedMessage ?: "ERROR"
            upAdapterLiveData.postValue(book.bookUrl)
            it.printOnDebug()
        }.onSuccess {
            exportProgress.remove(book.bookUrl)
            exportMsg[book.bookUrl] = context.getString(R.string.export_success)
            upAdapterLiveData.postValue(book.bookUrl)
        }.onFinally {
            exportNumber--
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun exportEpub(doc: DocumentFile, book: Book) {
        val filename = "${getExportFileName(book)}.epub"
        DocumentUtils.delete(doc, filename)
        val epubBook = EpubBook()
        epubBook.version = "2.0"
        //set metadata
        setEpubMetadata(book, epubBook)
        //set cover
        setCover(book, epubBook)
        //set css
        val contentModel = setAssets(doc, book, epubBook)

        //设置正文
        setEpubContent(contentModel, book, epubBook)
        DocumentUtils.createFileIfNotExist(doc, filename)?.let { bookDoc ->
            context.contentResolver.openOutputStream(bookDoc.uri, "wa")?.use { bookOs ->
                EpubWriter().write(epubBook, bookOs)
            }
            if (AppConfig.exportToWebDav) {
                // 导出到webdav
                AppWebDav.exportWebDav(bookDoc.uri, filename)
            }
        }
    }


    private suspend fun exportEpub(file: File, book: Book) {
        val filename = "${getExportFileName(book)}.epub"
        val epubBook = EpubBook()
        epubBook.version = "2.0"
        //set metadata
        setEpubMetadata(book, epubBook)
        //set cover
        setCover(book, epubBook)
        //set css
        val contentModel = setAssets(book, epubBook)

        val bookPath = FileUtils.getPath(file, filename)
        val bookFile = FileUtils.createFileWithReplace(bookPath)
        //设置正文
        setEpubContent(contentModel, book, epubBook)
        @Suppress("BlockingMethodInNonBlockingContext")
        EpubWriter().write(epubBook, FileOutputStream(bookFile))
        if (AppConfig.exportToWebDav) {
            // 导出到webdav
            AppWebDav.exportWebDav(Uri.fromFile(bookFile), filename)
        }
    }

    private fun setAssets(doc: DocumentFile, book: Book, epubBook: EpubBook): String {
        if (!(AppConfig.isGooglePlay || appCtx.packageName.contains(
                "debug",
                true
            ))
        ) return setAssets(book, epubBook)

        var contentModel = ""
        DocumentUtils.getDirDocument(doc, "Asset").let { customPath ->
            if (customPath == null) {//使用内置模板
                contentModel = setAssets(book, epubBook)
            } else {//外部模板
                customPath.listFiles().forEach { folder ->
                    if (folder.isDirectory && folder.name == "Text") {
                        folder.listFiles().sortedWith { o1, o2 ->
                            val name1 = o1.name ?: ""
                            val name2 = o2.name ?: ""
                            name1.cnCompare(name2)
                        }.forEach { file ->
                            if (file.isFile) {
                                when {
                                    //正文模板
                                    file.name.equals("chapter.html", true)
                                            || file.name.equals("chapter.xhtml", true) -> {
                                        contentModel = file.readText(context)
                                    }
                                    //封面等其他模板
                                    true == file.name?.endsWith("html", true) -> {
                                        epubBook.addSection(
                                            FileUtils.getNameExcludeExtension(
                                                file.name ?: "Cover.html"
                                            ),
                                            ResourceUtil.createPublicResource(
                                                book.name,
                                                book.getRealAuthor(),
                                                book.getDisplayIntro(),
                                                book.kind,
                                                book.wordCount,
                                                file.readText(context),
                                                "${folder.name}/${file.name}"
                                            )
                                        )
                                    }
                                    else -> {
                                        //其他格式文件当做资源文件
                                        folder.listFiles().forEach {
                                            if (it.isFile)
                                                epubBook.resources.add(
                                                    Resource(
                                                        it.readBytes(context),
                                                        "${folder.name}/${it.name}"
                                                    )
                                                )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (folder.isDirectory) {
                        //资源文件
                        folder.listFiles().forEach {
                            if (it.isFile)
                                epubBook.resources.add(
                                    Resource(
                                        it.readBytes(context),
                                        "${folder.name}/${it.name}"
                                    )
                                )
                        }
                    } else {//Asset下面的资源文件
                        epubBook.resources.add(
                            Resource(
                                folder.readBytes(context),
                                "${folder.name}"
                            )
                        )
                    }
                }
            }
        }

        return contentModel
    }

    private fun setAssets(book: Book, epubBook: EpubBook): String {
        epubBook.resources.add(
            Resource(
                appCtx.assets.open("epub/fonts.css").readBytes(),
                "Styles/fonts.css"
            )
        )
        epubBook.resources.add(
            Resource(
                appCtx.assets.open("epub/main.css").readBytes(),
                "Styles/main.css"
            )
        )
        epubBook.resources.add(
            Resource(
                appCtx.assets.open("epub/logo.png").readBytes(),
                "Images/logo.png"
            )
        )
        epubBook.addSection(
            context.getString(R.string.img_cover),
            ResourceUtil.createPublicResource(
                book.name,
                book.getRealAuthor(),
                book.getDisplayIntro(),
                book.kind,
                book.wordCount,
                String(appCtx.assets.open("epub/cover.html").readBytes()),
                "Text/cover.html"
            )
        )
        epubBook.addSection(
            context.getString(R.string.book_intro),
            ResourceUtil.createPublicResource(
                book.name,
                book.getRealAuthor(),
                book.getDisplayIntro(),
                book.kind,
                book.wordCount,
                String(appCtx.assets.open("epub/intro.html").readBytes()),
                "Text/intro.html"
            )
        )
        return String(appCtx.assets.open("epub/chapter.html").readBytes())
    }

    private fun setCover(book: Book, epubBook: EpubBook) {
        Glide.with(context)
            .asBitmap()
            .load(book.getDisplayCover())
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    val stream = ByteArrayOutputStream()
                    resource.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    val byteArray: ByteArray = stream.toByteArray()
                    resource.recycle()
                    stream.close()
                    epubBook.coverImage = Resource(byteArray, "Images/cover.jpg")
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })
    }

    private suspend fun setEpubContent(
        contentModel: String,
        book: Book,
        epubBook: EpubBook
    ) {
        //正文
        val useReplace = AppConfig.exportUseReplace && book.getUseReplaceRule()
        val contentProcessor = ContentProcessor.get(book.name, book.origin)
        appDb.bookChapterDao.getChapterList(book.bookUrl).forEachIndexed { index, chapter ->
            coroutineContext.ensureActive()
            upAdapterLiveData.postValue(book.bookUrl)
            exportProgress[book.bookUrl] = index
            BookHelp.getContent(book, chapter).let { content ->
                var content1 = fixPic(epubBook, book, content ?: "null", chapter)
                content1 = contentProcessor
                    .getContent(
                        book,
                        chapter,
                        content1,
                        includeTitle = false,
                        useReplace = useReplace,
                        chineseConvert = false,
                        reSegment = false
                    ).toString()
                val title = chapter.getDisplayTitle(
                    contentProcessor.getTitleReplaceRules(),
                    useReplace = useReplace
                )
                epubBook.addSection(
                    title,
                    ResourceUtil.createChapterResource(
                        title.replace("\uD83D\uDD12", ""),
                        content1,
                        contentModel,
                        "Text/chapter_${index}.html"
                    )
                )
            }
        }
    }

    private fun fixPic(
        epubBook: EpubBook,
        book: Book,
        content: String,
        chapter: BookChapter
    ): String {
        val data = StringBuilder("")
        content.split("\n").forEach { text ->
            var text1 = text
            val matcher = AppPattern.imgPattern.matcher(text)
            while (matcher.find()) {
                matcher.group(1)?.let {
                    val src = NetworkUtils.getAbsoluteURL(chapter.url, it)
                    val originalHref =
                        "${MD5Utils.md5Encode16(src)}.${BookHelp.getImageSuffix(src)}"
                    val href =
                        "Images/${MD5Utils.md5Encode16(src)}.${BookHelp.getImageSuffix(src)}"
                    val vFile = BookHelp.getImage(book, src)
                    val fp = FileResourceProvider(vFile.parent)
                    if (vFile.exists()) {
                        val img = LazyResource(fp, href, originalHref)
                        epubBook.resources.add(img)
                    }
                    text1 = text1.replace(src, "../${href}")
                }
            }
            data.append(text1).append("\n")
        }
        return data.toString()
    }

    private fun setEpubMetadata(book: Book, epubBook: EpubBook) {
        val metadata = Metadata()
        metadata.titles.add(book.name)//书籍的名称
        metadata.authors.add(Author(book.getRealAuthor()))//书籍的作者
        metadata.language = "zh"//数据的语言
        metadata.dates.add(Date())//数据的创建日期
        metadata.publishers.add("Legado")//数据的创建者
        metadata.descriptions.add(book.getDisplayIntro())//书籍的简介
        //metadata.subjects.add("")//书籍的主题，在静读天下里面有使用这个分类书籍
        epubBook.metadata = metadata
    }

    //////end of EPUB
}