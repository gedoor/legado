package io.legado.app.service

import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseService
import io.legado.app.constant.AppConst
import io.legado.app.constant.EventBus
import io.legado.app.constant.IntentAction
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.AppConfig
import io.legado.app.help.BookHelp
import io.legado.app.help.IntentHelp
import io.legado.app.help.coroutine.CompositeCoroutine
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.webBook.WebBook
import io.legado.app.service.help.CacheBook
import io.legado.app.utils.postEvent
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.isActive
import org.jetbrains.anko.toast
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors

class CacheBookService : BaseService() {
    private val threadCount = AppConfig.threadCount
    private var searchPool =
        Executors.newFixedThreadPool(threadCount).asCoroutineDispatcher()
    private var tasks = CompositeCoroutine()
    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable = Runnable { upDownload() }
    private val bookMap = ConcurrentHashMap<String, Book>()
    private val webBookMap = ConcurrentHashMap<String, WebBook>()
    private val downloadMap = ConcurrentHashMap<String, CopyOnWriteArraySet<BookChapter>>()
    private val downloadCount = ConcurrentHashMap<String, DownloadCount>()
    private val finalMap = ConcurrentHashMap<String, CopyOnWriteArraySet<BookChapter>>()
    private val downloadingList = CopyOnWriteArraySet<String>()

    @Volatile
    private var downloadingCount = 0
    private var notificationContent = App.INSTANCE.getString(R.string.starting_download)

    private val notificationBuilder by lazy {
        val builder = NotificationCompat.Builder(this, AppConst.channelIdDownload)
            .setSmallIcon(R.drawable.ic_download)
            .setOngoing(true)
            .setContentTitle(getString(R.string.offline_cache))
        builder.addAction(
            R.drawable.ic_stop_black_24dp,
            getString(R.string.cancel),
            IntentHelp.servicePendingIntent<CacheBookService>(this, IntentAction.stop)
        )
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    override fun onCreate() {
        super.onCreate()
        upNotification()
        handler.postDelayed(runnable, 1000)
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
        searchPool.close()
        handler.removeCallbacks(runnable)
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
                    book = App.db.bookDao.getBook(bookUrl)
                    if (book == null) {
                        removeDownload(bookUrl)
                    }
                }
            }
        }
        return book
    }

    private fun getWebBook(bookUrl: String, origin: String): WebBook? {
        var webBook = webBookMap[origin]
        if (webBook == null) {
            synchronized(this) {
                webBook = webBookMap[origin]
                if (webBook == null) {
                    App.db.bookSourceDao.getBookSource(origin)?.let {
                        webBook = WebBook(it)
                    }
                    if (webBook == null) {
                        removeDownload(bookUrl)
                    }
                }
            }
        }
        return webBook
    }

    private fun addDownloadData(bookUrl: String?, start: Int, end: Int) {
        bookUrl ?: return
        if (downloadMap.containsKey(bookUrl)) {
            notificationContent = getString(R.string.already_in_download)
            upNotification()
            toast(notificationContent)
            return
        }
        downloadCount[bookUrl] = DownloadCount()
        execute {
            App.db.bookChapterDao.getChapterList(bookUrl, start, end).let {
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
        val task = Coroutine.async(this, context = searchPool) {
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
                val webBook = getWebBook(bookChapter.bookUrl, book.origin)
                if (webBook == null) {
                    postDownloading(true)
                    return@async
                }
                if (!BookHelp.hasContent(book, bookChapter)) {
                    webBook.getContent(this, book, bookChapter, context = searchPool)
                        .timeout(60000L)
                        .onError {
                            synchronized(this) {
                                downloadingList.remove(bookChapter.url)
                            }
                            notificationContent = "getContentError${it.localizedMessage}"
                            upNotification()
                        }
                        .onSuccess {
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
                        }.onFinally {
                            postDownloading(true)
                        }
                } else {
                    //无需下载的，设置为增加成功
                    downloadCount[book.bookUrl]?.increaseSuccess()
                    downloadCount[book.bookUrl]?.increaseFinished()
                    postDownloading(true)
                }
            }
        }.onError {
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

    private fun upDownload() {
        upNotification()
        postEvent(EventBus.UP_DOWNLOAD, downloadMap)
        handler.removeCallbacks(runnable)
        handler.postDelayed(runnable, 1000)
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