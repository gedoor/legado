package io.legado.app.service

import android.content.Intent
import androidx.core.app.NotificationCompat
import io.legado.app.R
import io.legado.app.base.BaseService
import io.legado.app.constant.AppConst
import io.legado.app.constant.EventBus
import io.legado.app.constant.IntentAction
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.help.AppConfig
import io.legado.app.help.BookHelp
import io.legado.app.help.coroutine.CompositeCoroutine
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.CacheBook
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.postEvent
import io.legado.app.utils.servicePendingIntent
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import splitties.init.appCtx
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors
import kotlin.math.min

class CacheBookService : BaseService() {
    private val threadCount = AppConfig.threadCount
    private var cachePool =
        Executors.newFixedThreadPool(min(threadCount, 8)).asCoroutineDispatcher()
    private var tasks = CompositeCoroutine()
    private val bookMap = ConcurrentHashMap<String, Book>()
    private val bookSourceMap = ConcurrentHashMap<String, BookSource>()
    private val downloadMap = ConcurrentHashMap<String, CopyOnWriteArraySet<BookChapter>>()
    private val downloadCount = ConcurrentHashMap<String, DownloadCount>()
    private val finalMap = ConcurrentHashMap<String, CopyOnWriteArraySet<BookChapter>>()
    private val downloadingList = CopyOnWriteArraySet<String>()

    @Volatile
    private var downloadingCount = 0
    private var notificationContent = appCtx.getString(R.string.starting_download)

    private val notificationBuilder by lazy {
        val builder = NotificationCompat.Builder(this, AppConst.channelIdDownload)
            .setSmallIcon(R.drawable.ic_download)
            .setOngoing(true)
            .setContentTitle(getString(R.string.offline_cache))
        builder.addAction(
            R.drawable.ic_stop_black_24dp,
            getString(R.string.cancel),
            servicePendingIntent<CacheBookService>(IntentAction.stop)
        )
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    override fun onCreate() {
        super.onCreate()
        upNotification()
        launch {
            while (isActive) {
                delay(1000)
                upNotification()
                postEvent(EventBus.UP_DOWNLOAD, downloadMap)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                IntentAction.start -> addDownloadData(
                    intent.getStringExtra("bookUrl"),
                    intent.getIntExtra("start", 0),
                    intent.getIntExtra("end", 0)
                )
                IntentAction.remove -> removeDownload(intent.getStringExtra("bookUrl"))
                IntentAction.stop -> stopDownload()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        tasks.clear()
        cachePool.close()
        downloadMap.clear()
        finalMap.clear()
        super.onDestroy()
        postEvent(EventBus.UP_DOWNLOAD, downloadMap)
    }

    private fun getBook(bookUrl: String): Book? {
        var book = bookMap[bookUrl]
        if (book == null) {
            synchronized(this) {
                book = bookMap[bookUrl]
                if (book == null) {
                    book = appDb.bookDao.getBook(bookUrl)
                    if (book == null) {
                        removeDownload(bookUrl)
                    }
                }
            }
        }
        return book
    }

    private fun getBookSource(bookUrl: String, origin: String): BookSource? {
        var bookSource = bookSourceMap[origin]
        if (bookSource == null) {
            synchronized(this) {
                bookSource = bookSourceMap[origin]
                if (bookSource == null) {
                    bookSource = appDb.bookSourceDao.getBookSource(origin)
                    if (bookSource == null) {
                        removeDownload(bookUrl)
                    }
                }
            }
        }
        return bookSource
    }

    private fun addDownloadData(bookUrl: String?, start: Int, end: Int) {
        bookUrl ?: return
        if (downloadMap.containsKey(bookUrl)) {
            notificationContent = getString(R.string.already_in_download)
            upNotification()
            toastOnUi(notificationContent)
            return
        }
        downloadCount[bookUrl] = DownloadCount()
        execute(context = cachePool) {
            appDb.bookChapterDao.getChapterList(bookUrl, start, end).let {
                if (it.isNotEmpty()) {
                    val chapters = CopyOnWriteArraySet<BookChapter>()
                    chapters.addAll(it)
                    downloadMap[bookUrl] = chapters
                } else {
                    CacheBook.addLog("${getBook(bookUrl)?.name} is empty")
                }
            }
            for (i in 0 until threadCount) {
                if (downloadingCount < threadCount) {
                    download()
                }
            }
        }
    }

    private fun removeDownload(bookUrl: String?) {
        downloadMap.remove(bookUrl)
        finalMap.remove(bookUrl)
    }

    private fun download() {
        downloadingCount += 1
        val task = Coroutine.async(this, context = cachePool) {
            if (!isActive) return@async
            val bookChapter: BookChapter? = synchronized(this@CacheBookService) {
                downloadMap.forEach {
                    it.value.forEach { chapter ->
                        if (!downloadingList.contains(chapter.url)) {
                            downloadingList.add(chapter.url)
                            return@synchronized chapter
                        }
                    }
                }
                return@synchronized null
            }
            if (bookChapter == null) {
                postDownloading(false)
            } else {
                val book = getBook(bookChapter.bookUrl)
                if (book == null) {
                    postDownloading(true)
                    return@async
                }
                val bookSource = getBookSource(bookChapter.bookUrl, book.origin)
                if (bookSource == null) {
                    postDownloading(true)
                    return@async
                }
                if (!BookHelp.hasImageContent(book, bookChapter)) {
                    WebBook.getContent(this, bookSource, book, bookChapter, context = cachePool)
                        .timeout(60000L)
                        .onError(cachePool) {
                            synchronized(this) {
                                downloadingList.remove(bookChapter.url)
                            }
                            notificationContent = "getContentError${it.localizedMessage}"
                            upNotification()
                        }
                        .onSuccess(cachePool) {
                            synchronized(this@CacheBookService) {
                                downloadCount[book.bookUrl]?.increaseSuccess()
                                downloadCount[book.bookUrl]?.increaseFinished()
                                downloadCount[book.bookUrl]?.let {
                                    upNotification(
                                        it,
                                        downloadMap[book.bookUrl]?.size,
                                        bookChapter.title
                                    )
                                }
                                val chapterMap =
                                    finalMap[book.bookUrl]
                                        ?: CopyOnWriteArraySet<BookChapter>().apply {
                                            finalMap[book.bookUrl] = this
                                        }
                                chapterMap.add(bookChapter)
                                if (chapterMap.size == downloadMap[book.bookUrl]?.size) {
                                    downloadMap.remove(book.bookUrl)
                                    finalMap.remove(book.bookUrl)
                                    downloadCount.remove(book.bookUrl)
                                }
                            }
                        }.onFinally(cachePool) {
                            postDownloading(true)
                        }
                } else {
                    //无需下载的，设置为增加成功
                    downloadCount[book.bookUrl]?.increaseSuccess()
                    downloadCount[book.bookUrl]?.increaseFinished()
                    postDownloading(true)
                }
            }
        }.onError(cachePool) {
            notificationContent = "ERROR:${it.localizedMessage}"
            CacheBook.addLog(notificationContent)
            upNotification()
        }
        tasks.add(task)
    }

    private fun postDownloading(hasChapter: Boolean) {
        downloadingCount -= 1
        if (hasChapter) {
            download()
        } else {
            if (downloadingCount < 1) {
                stopDownload()
            }
        }
    }

    private fun stopDownload() {
        tasks.clear()
        stopSelf()
    }

    private fun upNotification(
        downloadCount: DownloadCount,
        totalCount: Int?,
        content: String
    ) {
        notificationContent =
            "进度:" + downloadCount.downloadFinishedCount + "/" + totalCount + ",成功:" + downloadCount.successCount + "," + content
    }

    /**
     * 更新通知
     */
    private fun upNotification() {
        notificationBuilder.setContentText(notificationContent)
        val notification = notificationBuilder.build()
        startForeground(AppConst.notificationIdDownload, notification)
    }

    class DownloadCount {
        @Volatile
        var downloadFinishedCount = 0 // 下载完成的条目数量

        @Volatile
        var successCount = 0 //下载成功的条目数量

        fun increaseSuccess() {
            ++successCount
        }

        fun increaseFinished() {
            ++downloadFinishedCount
        }
    }
}