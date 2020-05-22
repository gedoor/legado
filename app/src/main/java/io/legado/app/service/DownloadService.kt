package io.legado.app.service

import android.content.Intent
import android.os.Handler
import androidx.core.app.NotificationCompat
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseService
import io.legado.app.constant.AppConst
import io.legado.app.constant.EventBus
import io.legado.app.constant.IntentAction
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.AppConfig
import io.legado.app.help.BookHelp
import io.legado.app.help.IntentHelp
import io.legado.app.help.coroutine.CompositeCoroutine
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.WebBook
import io.legado.app.utils.postEvent
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.isActive
import org.jetbrains.anko.toast
import java.util.concurrent.Executors

class DownloadService : BaseService() {
    private var searchPool =
        Executors.newFixedThreadPool(AppConfig.threadCount).asCoroutineDispatcher()
    private var tasks = CompositeCoroutine()
    private val handler = Handler()
    private var runnable: Runnable = Runnable { upDownload() }
    private val downloadMap = hashMapOf<String, LinkedHashSet<BookChapter>>()
    private val downloadCount = hashMapOf<String, DownloadCount>()
    private val finalMap = hashMapOf<String, LinkedHashSet<BookChapter>>()
    private var notificationContent = "正在启动下载"

    private val notificationBuilder by lazy {
        val builder = NotificationCompat.Builder(this, AppConst.channelIdDownload)
            .setSmallIcon(R.drawable.ic_download)
            .setOngoing(true)
            .setContentTitle(getString(R.string.download_offline))
        builder.addAction(
            R.drawable.ic_stop_black_24dp,
            getString(R.string.cancel),
            IntentHelp.servicePendingIntent<DownloadService>(this, IntentAction.stop)
        )
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    override fun onCreate() {
        super.onCreate()
        updateNotification(notificationContent)
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

    private fun addDownloadData(bookUrl: String?, start: Int, end: Int) {
        bookUrl ?: return
        if (downloadMap.containsKey(bookUrl)) {
            toast("该书已在下载列表")
            return
        }
        execute {
            val chapterMap = downloadMap[bookUrl] ?: linkedSetOf<BookChapter>().apply {
                downloadMap[bookUrl] = this
            }
            App.db.bookChapterDao().getChapterList(bookUrl, start, end).let {
                chapterMap.addAll(it)
            }
            download()
        }
    }

    private fun removeDownload(bookUrl: String?) {
        downloadMap.remove(bookUrl)
        finalMap.remove(bookUrl)
    }

    private fun updateNotification(downloadCount:DownloadCount, totalCount: Int, content: String){
        notificationContent =
            "进度:${downloadCount.downloadFinishedCount}/$totalCount,成功:${downloadCount.successCount},$content"
    }

    private fun download() {
        val task = Coroutine.async(this, context = searchPool) {
            downloadMap.forEach { entry ->
                if (!isActive) return@async
                if (!finalMap.containsKey(entry.key)) {
                    val book = App.db.bookDao().getBook(entry.key) ?: return@async
                    val bookSource =
                        App.db.bookSourceDao().getBookSource(book.origin) ?: return@async
                    val webBook = WebBook(bookSource)

                    downloadCount[entry.key] = DownloadCount()

                    entry.value.forEach { chapter ->
                        if (!isActive) return@async
                        if (downloadMap.containsKey(book.bookUrl)) {
                            if (!BookHelp.hasContent(book, chapter)) {
                                webBook.getContent(
                                    book,
                                    chapter,
                                    scope = this,
                                    context = searchPool
                                ).onSuccess(IO) { content ->
                                        downloadCount[entry.key]?.increaseSuccess()
                                        BookHelp.saveContent(book, chapter, content)
                                    }
                                    .onFinally(IO) {
                                        synchronized(this@DownloadService) {
                                            downloadCount[entry.key]?.increaseFinished()
                                            downloadCount[entry.key]?.let { updateNotification(it, entry.value.size, chapter.title) }
                                            val chapterMap =
                                                finalMap[book.bookUrl]
                                                    ?: linkedSetOf<BookChapter>().apply {
                                                        finalMap[book.bookUrl] = this
                                                    }
                                            chapterMap.add(chapter)
                                            if (chapterMap.size == entry.value.size) {
                                                downloadMap.remove(book.bookUrl)
                                                finalMap.remove(book.bookUrl)
                                                downloadCount.remove(entry.key)
                                            }
                                        }
                                    }
                            } else{
                                //无需下载的，设置为增加成功
                                downloadCount[entry.key]?.increaseSuccess()
                                downloadCount[entry.key]?.increaseFinished()
                            }
                        }
                    }
                }

            }
        }

        tasks.add(task)
        task.invokeOnCompletion {
            tasks.remove(task)
            if (tasks.isEmpty) {
                stopSelf()
            }
        }
    }

    private fun stopDownload() {
        tasks.clear()
        stopSelf()
    }

    private fun upDownload() {
        updateNotification(notificationContent)
        postEvent(EventBus.UP_DOWNLOAD, downloadMap)
        handler.removeCallbacks(runnable)
        handler.postDelayed(runnable, 1000)
    }

    /**
     * 更新通知
     */
    private fun updateNotification(content: String) {
        val builder = notificationBuilder
        builder.setContentText(content)
        val notification = builder.build()
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