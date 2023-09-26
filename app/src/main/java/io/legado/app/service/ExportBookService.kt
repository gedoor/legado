package io.legado.app.service

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.util.ArraySet
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import io.legado.app.R
import io.legado.app.base.BaseService
import io.legado.app.constant.AppConst
import io.legado.app.constant.AppLog
import io.legado.app.constant.AppPattern
import io.legado.app.constant.EventBus
import io.legado.app.constant.IntentAction
import io.legado.app.constant.NotificationId
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.AppWebDav
import io.legado.app.help.book.BookHelp
import io.legado.app.help.book.ContentProcessor
import io.legado.app.help.book.getExportFileName
import io.legado.app.help.config.AppConfig
import io.legado.app.help.coroutine.OrderCoroutine
import io.legado.app.ui.book.cache.CacheActivity
import io.legado.app.utils.DocumentUtils
import io.legado.app.utils.FileUtils
import io.legado.app.utils.HtmlFormatter
import io.legado.app.utils.MD5Utils
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.activityPendingIntent
import io.legado.app.utils.buildMainHandler
import io.legado.app.utils.cnCompare
import io.legado.app.utils.createFolderIfNotExist
import io.legado.app.utils.isContentScheme
import io.legado.app.utils.postEvent
import io.legado.app.utils.readBytes
import io.legado.app.utils.readText
import io.legado.app.utils.servicePendingIntent
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.writeBytes
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import me.ag2s.epublib.domain.Author
import me.ag2s.epublib.domain.Date
import me.ag2s.epublib.domain.EpubBook
import me.ag2s.epublib.domain.FileResourceProvider
import me.ag2s.epublib.domain.LazyResource
import me.ag2s.epublib.domain.Metadata
import me.ag2s.epublib.domain.Resource
import me.ag2s.epublib.epub.EpubWriter
import me.ag2s.epublib.epub.EpubWriterProcessor
import me.ag2s.epublib.util.ResourceUtil
import splitties.init.appCtx
import splitties.systemservices.notificationManager
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

/**
 * 导出书籍服务
 */
class ExportBookService : BaseService() {

    companion object {
        val exportProgress = ConcurrentHashMap<String, Int>()
        val exportMsg = ConcurrentHashMap<String, String>()
    }

    data class ExportConfig(
        val path: String,
        val type: String,
        val epubSize: Int = 1,
        val epubScope: String? = null
    )

    private val groupKey = "${appCtx.packageName}.exportBook"
    private val waitExportBooks = linkedMapOf<String, ExportConfig>()
    private var exportJob: Job? = null
    private var notificationContentText = appCtx.getString(R.string.service_starting)


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            IntentAction.start -> kotlin.runCatching {
                val bookUrl = intent.getStringExtra("bookUrl")!!
                if (!exportProgress.contains(bookUrl)) {
                    val exportConfig = ExportConfig(
                        path = intent.getStringExtra("exportPath")!!,
                        type = intent.getStringExtra("exportType")!!,
                        epubSize = intent.getIntExtra("epubSize", 1),
                        epubScope = intent.getStringExtra("epubScope")
                    )
                    waitExportBooks[bookUrl] = exportConfig
                    exportMsg[bookUrl] = getString(R.string.export_wait)
                    postEvent(EventBus.EXPORT_BOOK, bookUrl)
                    export()
                }
            }.onFailure {
                toastOnUi(it.localizedMessage)
            }

            IntentAction.stop -> stopSelf()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        exportProgress.clear()
        exportMsg.clear()
    }

    @SuppressLint("MissingPermission")
    override fun upNotification() {
        val notification = NotificationCompat.Builder(this, AppConst.channelIdDownload)
            .setSmallIcon(R.drawable.ic_export)
            .setSubText(getString(R.string.export_book))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setGroup(groupKey)
            .setGroupSummary(true)
        startForeground(NotificationId.ExportBookService, notification.build())
    }

    private fun upExportNotification() {
        val notification = NotificationCompat.Builder(this, AppConst.channelIdDownload)
            .setSmallIcon(R.drawable.ic_export)
            .setSubText(getString(R.string.export_book))
            .setContentIntent(activityPendingIntent<CacheActivity>("cacheActivity"))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentText(notificationContentText)
            .setDeleteIntent(servicePendingIntent<ExportBookService>(IntentAction.stop))
            .setGroup(groupKey)
        if (exportJob?.isActive == true) {
            notification.setOngoing(true)
            notification.addAction(
                R.drawable.ic_stop_black_24dp,
                getString(R.string.cancel),
                servicePendingIntent<ExportBookService>(IntentAction.stop)
            )
        }
        notificationManager.notify(NotificationId.ExportBook, notification.build())
    }

    private fun export() {
        if (exportJob?.isActive == true) {
            return
        }
        exportJob = lifecycleScope.launch(IO) {
            while (true) {
                val (bookUrl, exportConfig) = waitExportBooks.entries.firstOrNull() ?: let {
                    notificationContentText = "导出完成"
                    upExportNotification()
                    return@launch
                }
                exportProgress[bookUrl] = 0
                waitExportBooks.remove(bookUrl)
                val book = appDb.bookDao.getBook(bookUrl)
                try {
                    book ?: throw NoStackTraceException("获取${bookUrl}书籍出错")
                    notificationContentText = getString(
                        R.string.export_book_notification_content,
                        book.name,
                        waitExportBooks.size
                    )
                    upExportNotification()
                    if (exportConfig.type == "epub") {
                        if (exportConfig.epubScope.isNullOrBlank()) {
                            exportEPUB(exportConfig.path, book)
                        } else {
                            CustomExporter(
                                paresScope(exportConfig.epubScope),
                                exportConfig.epubSize
                            ).export(exportConfig.path, book)
                        }
                    } else {
                        export(exportConfig.path, book)
                    }
                    exportMsg[book.bookUrl] = getString(R.string.export_success)
                } catch (e: Throwable) {
                    exportMsg[bookUrl] = e.localizedMessage ?: "ERROR"
                    AppLog.put("导出书籍<${book?.name ?: bookUrl}>出错", e)
                } finally {
                    exportProgress.remove(bookUrl)
                    postEvent(EventBus.EXPORT_BOOK, bookUrl)
                }
            }
        }
    }

    private data class SrcData(
        val chapterTitle: String,
        val index: Int,
        val src: String
    )

    private suspend fun export(path: String, book: Book) {
        exportMsg.remove(book.bookUrl)
        postEvent(EventBus.EXPORT_BOOK, book.bookUrl)
        if (path.isContentScheme()) {
            val uri = Uri.parse(path)
            val doc = DocumentFile.fromTreeUri(this@ExportBookService, uri)
                ?: throw NoStackTraceException("获取导出文档失败")
            export(doc, book)
        } else {
            export(File(path).createFolderIfNotExist(), book)
        }
    }

    private suspend fun export(doc: DocumentFile, book: Book) {
        val filename = book.getExportFileName("txt")
        DocumentUtils.delete(doc, filename)
        val bookDoc = DocumentUtils.createFileIfNotExist(doc, filename)
            ?: throw NoStackTraceException("创建文档失败，请尝试重新设置导出文件夹")
        contentResolver.openOutputStream(bookDoc.uri, "wa")?.use { bookOs ->
            getAllContents(book) { text, srcList ->
                bookOs.write(text.toByteArray(Charset.forName(AppConfig.exportCharset)))
                srcList?.forEach {
                    val vFile = BookHelp.getImage(book, it.src)
                    if (vFile.exists()) {
                        DocumentUtils.createFileIfNotExist(
                            doc,
                            "${it.index}-${MD5Utils.md5Encode16(it.src)}.jpg",
                            subDirs = arrayOf(
                                "${book.name}_${book.author}",
                                "images",
                                it.chapterTitle
                            )
                        )?.writeBytes(this, vFile.readBytes())
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
        val filename = book.getExportFileName("txt")
        val bookPath = FileUtils.getPath(file, filename)
        val bookFile = FileUtils.createFileWithReplace(bookPath)
        getAllContents(book) { text, srcList ->
            bookFile.appendText(text, Charset.forName(AppConfig.exportCharset))
            srcList?.forEach {
                val vFile = BookHelp.getImage(book, it.src)
                if (vFile.exists()) {
                    FileUtils.createFileIfNotExist(
                        file,
                        "${book.name}_${book.author}",
                        "images",
                        it.chapterTitle,
                        "${it.index}-${MD5Utils.md5Encode16(it.src)}.jpg"
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
        append: (text: String, srcList: ArrayList<SrcData>?) -> Unit
    ) {
        val useReplace = AppConfig.exportUseReplace && book.getUseReplaceRule()
        val contentProcessor = ContentProcessor.get(book.name, book.origin)
        val qy = "${book.name}\n${
            getString(R.string.author_show, book.getRealAuthor())
        }\n${
            getString(
                R.string.intro_show,
                "\n" + HtmlFormatter.format(book.getDisplayIntro())
            )
        }"
        append(qy, null)
        if (AppConfig.parallelExportBook) {
            val oc =
                OrderCoroutine<Pair<String, ArrayList<SrcData>?>>(AppConfig.threadCount)
            appDb.bookChapterDao.getChapterList(book.bookUrl).forEach { chapter ->
                oc.submit { getExportData(book, chapter, contentProcessor, useReplace) }
            }
            oc.collect { index, result ->
                postEvent(EventBus.EXPORT_BOOK, book.bookUrl)
                exportProgress[book.bookUrl] = index
                append.invoke(result.first, result.second)
            }
        } else {
            appDb.bookChapterDao.getChapterList(book.bookUrl).forEachIndexed { index, chapter ->
                coroutineContext.ensureActive()
                postEvent(EventBus.EXPORT_BOOK, book.bookUrl)
                exportProgress[book.bookUrl] = index
                val result = getExportData(book, chapter, contentProcessor, useReplace)
                append.invoke(result.first, result.second)
            }
        }

    }

    private fun getExportData(
        book: Book,
        chapter: BookChapter,
        contentProcessor: ContentProcessor,
        useReplace: Boolean
    ): Pair<String, ArrayList<SrcData>?> {
        BookHelp.getContent(book, chapter).let { content ->
            val content1 = contentProcessor
                .getContent(
                    book,
                    // 不导出vip标识
                    chapter.apply { isVip = false },
                    content ?: if (chapter.isVolume) "" else "null",
                    includeTitle = !AppConfig.exportNoChapterName,
                    useReplace = useReplace,
                    chineseConvert = false,
                    reSegment = false
                ).toString()
            if (AppConfig.exportPictureFile) {
                //txt导出图片文件
                val srcList = arrayListOf<SrcData>()
                content?.split("\n")?.forEachIndexed { index, text ->
                    val matcher = AppPattern.imgPattern.matcher(text)
                    while (matcher.find()) {
                        matcher.group(1)?.let {
                            val src = NetworkUtils.getAbsoluteURL(chapter.url, it)
                            srcList.add(SrcData(chapter.title, index, src))
                        }
                    }
                }
                return Pair("\n\n$content1", srcList)
            } else {
                return Pair("\n\n$content1", null)
            }
        }
    }

    /**
     * 解析范围字符串
     *
     * @param scope 范围字符串
     * @return 范围
     *
     * @since 2023/5/22
     * @author Discut
     */
    @SuppressLint("ObsoleteSdkInt")
    private fun paresScope(scope: String): IntArray {
        val split = scope.split(",")

        @Suppress("RemoveExplicitTypeArguments")
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArraySet<Int>()
        } else {
            HashSet<Int>()
        }
        for (s in split) {
            val v = s.split("-")
            if (v.size != 2) {
                result.add(s.toInt() - 1)
                continue
            }
            val left = v[0].toInt()
            val right = v[1].toInt()
            if (left > right) {
                AppLog.put("Error expression : $s; left > right")
                continue
            }
            for (i in left..right)
                result.add(i - 1)
        }
        return result.toIntArray()
    }

    /**
     * 导出Epub
     */
    private suspend fun exportEPUB(path: String, book: Book) {
        exportMsg.remove(book.bookUrl)
        postEvent(EventBus.EXPORT_BOOK, book.bookUrl)
        if (path.isContentScheme()) {
            val uri = Uri.parse(path)
            val doc = DocumentFile.fromTreeUri(this@ExportBookService, uri)
                ?: throw NoStackTraceException("获取导出文档失败")
            exportEpub(doc, book)
        } else {
            exportEpub(File(path).createFolderIfNotExist(), book)
        }
    }

    private suspend fun exportEpub(doc: DocumentFile, book: Book) {
        val filename = book.getExportFileName("epub")
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
            contentResolver.openOutputStream(bookDoc.uri, "wa")?.use { bookOs ->
                EpubWriter().write(epubBook, BufferedOutputStream(bookOs))
            }
            if (AppConfig.exportToWebDav) {
                // 导出到webdav
                AppWebDav.exportWebDav(bookDoc.uri, filename)
            }
        }
    }


    private suspend fun exportEpub(file: File, book: Book) {
        val filename = book.getExportFileName("epub")
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
        EpubWriter().write(epubBook, BufferedOutputStream(FileOutputStream(bookFile)))
        if (AppConfig.exportToWebDav) {
            // 导出到webdav
            AppWebDav.exportWebDav(Uri.fromFile(bookFile), filename)
        }
    }

    private fun setAssets(doc: DocumentFile, book: Book, epubBook: EpubBook): String {
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
                                        contentModel = file.readText(this)
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
                                                file.readText(this),
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
                                                        it.readBytes(this),
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
                                        it.readBytes(this),
                                        "${folder.name}/${it.name}"
                                    )
                                )
                        }
                    } else {//Asset下面的资源文件
                        epubBook.resources.add(
                            Resource(
                                folder.readBytes(this),
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
            getString(R.string.img_cover),
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
            getString(R.string.book_intro),
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
        Glide.with(this)
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
            postEvent(EventBus.EXPORT_BOOK, book.bookUrl)
            exportProgress[book.bookUrl] = index
            BookHelp.getContent(book, chapter).let { content ->
                var content1 = fixPic(
                    epubBook,
                    book,
                    content ?: if (chapter.isVolume) "" else "null",
                    chapter
                )
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
                val title = chapter.run {
                    // 不导出vip标识
                    isVip = false
                    getDisplayTitle(
                        contentProcessor.getTitleReplaceRules(),
                        useReplace = useReplace
                    )
                }
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

    //////start of custom exporter
    /**
     * 自定义Exporter
     * @param scope 导出范围
     * @param size epub 文件包含最大章节数
     */
    inner class CustomExporter(private val scope: IntArray, private val size: Int) {

        /**
         * 导出Epub
         * @param path 导出的路径
         * @param book 书籍
         */
        suspend fun export(
            path: String,
            book: Book
        ) {
            exportProgress[book.bookUrl] = 0
            exportMsg.remove(book.bookUrl)
            postEvent(EventBus.EXPORT_BOOK, book.bookUrl)
            val currentTimeMillis = System.currentTimeMillis()
            when (path.isContentScheme()) {
                true -> {
                    val uri = Uri.parse(path)
                    val doc = DocumentFile.fromTreeUri(this@ExportBookService, uri)
                        ?: throw NoStackTraceException("获取导出文档失败")
                    val (contentModel, epubList) = createEpubs(book, doc)
                    val asyncBlocks = ArrayList<Deferred<Unit>>(epubList.size)
                    var progressBar = 0.0
                    epubList.forEachIndexed { index, ep ->
                        val (filename, epubBook) = ep
                        coroutineScope {
                            val asyncBlock = async {
                                //设置正文
                                setEpubContent(
                                    contentModel,
                                    book,
                                    epubBook,
                                    index
                                ) { _, _ ->
                                    // 将章节写入内存时更新进度条
                                    postEvent(EventBus.EXPORT_BOOK, book.bookUrl)
                                    progressBar += book.totalChapterNum.toDouble() / scope.size / 2
                                    exportProgress[book.bookUrl] = progressBar.toInt()
                                }
                                save2Drive(filename, epubBook, doc) { total, _ ->
                                    //写入硬盘时更新进度条
                                    progressBar += book.totalChapterNum.toDouble() / epubList.size / total / 2
                                    postEvent(EventBus.EXPORT_BOOK, book.bookUrl)
                                    exportProgress[book.bookUrl] = progressBar.toInt()
                                }
                            }
                            asyncBlocks.add(asyncBlock)
                        }
                    }
                    asyncBlocks.forEach { it.await() }
                }

                false -> {
                    val file = File(path).createFolderIfNotExist()
                    val (contentModel, epubList) = createEpubs(book, null)
                    val asyncBlocks = ArrayList<Deferred<Unit>>(epubList.size)
                    var progressBar = 0.0
                    epubList.forEachIndexed { index, ep ->
                        val (filename, epubBook) = ep
                        coroutineScope {
                            val asyncBlock = async {
                                //设置正文
                                setEpubContent(
                                    contentModel,
                                    book,
                                    epubBook,
                                    index
                                ) { _, _ ->
                                    postEvent(EventBus.EXPORT_BOOK, book.bookUrl)
                                    exportProgress[book.bookUrl] =
                                        exportProgress[book.bookUrl]?.plus(book.totalChapterNum / scope.size)
                                            ?: 1
                                }
                                save2Drive(filename, epubBook, file) { total, _ ->
                                    //设置进度
                                    progressBar += book.totalChapterNum.toDouble() / epubList.size / total / 2
                                    postEvent(EventBus.EXPORT_BOOK, book.bookUrl)
                                    exportProgress[book.bookUrl] = progressBar.toInt()
                                }
                            }
                            asyncBlocks.add(asyncBlock)
                        }
                    }
                    asyncBlocks.forEach { it.await() }
                }
            }
            AppLog.put("分割导出书籍 ${book.name} 一共耗时 ${System.currentTimeMillis() - currentTimeMillis}")
        }


        /**
         * 设置epub正文
         *
         * @param contentModel 正文模板
         * @param book 书籍
         * @param epubBook 分割后的epub
         * @param epubBookIndex 分割后的epub序号
         */
        private suspend fun setEpubContent(
            contentModel: String,
            book: Book,
            epubBook: EpubBook,
            epubBookIndex: Int,
            updateProgress: (chapterList: MutableList<BookChapter>, index: Int) -> Unit
        ) {
            //正文
            val useReplace = AppConfig.exportUseReplace && book.getUseReplaceRule()
            val contentProcessor = ContentProcessor.get(book.name, book.origin)
            var chapterList: MutableList<BookChapter> = ArrayList()
            appDb.bookChapterDao.getChapterList(book.bookUrl).forEachIndexed { index, chapter ->
                if (scope.indexOf(index) >= 0) {
                    chapterList.add(chapter)
                }
                if (scope.size == chapterList.size) {
                    return@forEachIndexed
                }
            }
            // val totalChapterNum = book.totalChapterNum / scope.size
            if (chapterList.size == 0) {
                throw RuntimeException("书籍<${book.name}>(${epubBookIndex + 1})未找到章节信息")
            }
            chapterList = chapterList.subList(
                epubBookIndex * size,
                if ((epubBookIndex + 1) * size > scope.size) scope.size else (epubBookIndex + 1) * size
            )
            chapterList.forEachIndexed { index, chapter ->
                coroutineContext.ensureActive()
                updateProgress(chapterList, index)
                BookHelp.getContent(book, chapter).let { content ->
                    var content1 = fixPic(
                        epubBook,
                        book,
                        content ?: if (chapter.isVolume) "" else "null",
                        chapter
                    )
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
                    val title = chapter.run {
                        // 不导出vip标识
                        isVip = false
                        getDisplayTitle(
                            contentProcessor.getTitleReplaceRules(),
                            useReplace = useReplace
                        )
                    }
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

        /**
         * 创建多个epub 对象
         *
         * 分割epub时，一个书籍需要创建多个epub对象
         * @param doc 导出文档
         * @param book 书籍
         *
         * @return <内容模板字符串, <epub文件名, epub对象>>
         */
        private fun createEpubs(
            book: Book,
            doc: DocumentFile?,
        ): Pair<String, List<Pair<String, EpubBook>>> {
            val paresNumOfEpub = paresNumOfEpub(scope.size, size)
            val result: MutableList<Pair<String, EpubBook>> = ArrayList(paresNumOfEpub)
            var contentModel = ""
            for (i in 1..paresNumOfEpub) {
                val filename = book.getExportFileName("epub", i)
                doc?.let {
                    DocumentUtils.delete(it, filename)
                }
                val epubBook = EpubBook()
                epubBook.version = "2.0"
                //set metadata
                setEpubMetadata(book, epubBook)
                //set cover
                setCover(book, epubBook)
                //set css
                contentModel = doc?.let {
                    setAssets(it, book, epubBook)
                } ?: setAssets(book, epubBook)

                // add epubBook
                result.add(Pair(filename, epubBook))
            }
            return Pair(contentModel, result)
        }

        /**
         * 保存文件到 设备
         */
        private suspend fun save2Drive(
            filename: String,
            epubBook: EpubBook,
            doc: DocumentFile,
            callback: (total: Int, progress: Int) -> Unit
        ) {
            DocumentUtils.createFileIfNotExist(doc, filename)?.let { bookDoc ->
                contentResolver.openOutputStream(bookDoc.uri, "wa")?.use { bookOs ->
                    EpubWriter()
                        .setCallback(object : EpubWriterProcessor.Callback {
                            override fun onProgressing(total: Int, progress: Int) {
                                callback(total, progress)
                            }
                        })
                        .write(epubBook, BufferedOutputStream(bookOs))
                }
                if (AppConfig.exportToWebDav) {
                    // 导出到webdav
                    AppWebDav.exportWebDav(bookDoc.uri, filename)
                }
            }
        }

        /**
         * 保存文件到 设备
         */
        private suspend fun save2Drive(
            filename: String,
            epubBook: EpubBook,
            file: File,
            callback: (total: Int, progress: Int) -> Unit
        ) {
            val bookPath = FileUtils.getPath(file, filename)
            val bookFile = FileUtils.createFileWithReplace(bookPath)
            @Suppress("BlockingMethodInNonBlockingContext")
            EpubWriter()
                .setCallback(object : EpubWriterProcessor.Callback {
                    override fun onProgressing(total: Int, progress: Int) {
                        callback(total, progress)
                    }
                })
                .write(epubBook, BufferedOutputStream(FileOutputStream(bookFile)))
            if (AppConfig.exportToWebDav) {
                // 导出到webdav
                AppWebDav.exportWebDav(Uri.fromFile(bookFile), filename)
            }
        }

        /**
         * 解析 分割epub后的数量
         *
         * @param total 章节总数
         * @param size 每个epub文件包含多少章节
         */
        private fun paresNumOfEpub(total: Int, size: Int): Int {
            val i = total % size
            var result = total / size
            if (i > 0) {
                result++
            }
            return result
        }
    }
}