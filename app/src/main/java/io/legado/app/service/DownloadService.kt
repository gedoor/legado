package io.legado.app.service

import android.content.Intent
import android.os.Handler
import androidx.core.app.NotificationCompat
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseService
import io.legado.app.constant.Action
import io.legado.app.constant.AppConst
import io.legado.app.constant.Bus
import io.legado.app.help.BookHelp
import io.legado.app.help.IntentHelp
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.WebBook
import io.legado.app.utils.postEvent
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

class DownloadService : BaseService() {
    private var searchPool = Executors.newFixedThreadPool(16).asCoroutineDispatcher()
    private var tasks: ArrayList<Coroutine<*>> = arrayListOf()
    private val handler = Handler()
    private var runnable: Runnable = Runnable { upDownload() }
    private var notificationContent = "正在启动下载"
    private val notificationBuilder by lazy {
        val builder = NotificationCompat.Builder(this, AppConst.channelIdDownload)
            .setSmallIcon(R.drawable.ic_download)
            .setOngoing(true)
            .setContentTitle(getString(R.string.download_offline))
        builder.addAction(
            R.drawable.ic_stop_black_24dp,
            getString(R.string.cancel),
            IntentHelp.servicePendingIntent<DownloadService>(this, Action.stop)
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
                Action.start -> download(
                    intent.getStringExtra("bookUrl"),
                    intent.getIntExtra("start", 0),
                    intent.getIntExtra("end", 0)
                )
                Action.stop -> stopDownload()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        tasks.clear()
        searchPool.close()
        handler.removeCallbacks(runnable)
        super.onDestroy()
        postEvent(Bus.UP_DOWNLOAD, false)
    }

    private fun download(bookUrl: String?, start: Int, end: Int) {
        if (bookUrl == null) return
        val task = Coroutine.async(this) {
            val book = App.db.bookDao().getBook(bookUrl) ?: return@async
            val bookSource = App.db.bookSourceDao().getBookSource(book.origin) ?: return@async
            val webBook = WebBook(bookSource)
            for (index in start..end) {
                App.db.bookChapterDao().getChapter(bookUrl, index)?.let { chapter ->
                    if (!BookHelp.hasContent(book, chapter)) {
                        webBook.getContent(book, chapter, scope = this, context = searchPool)
                            .onStart {
                                notificationContent = chapter.title
                            }
                            .onSuccess(IO) { content ->
                                content?.let {
                                    BookHelp.saveContent(book, chapter, content)
                                }
                            }
                    }
                }
            }
        }
        tasks.add(task)
        task.invokeOnCompletion {
            tasks.remove(task)
            if (tasks.isEmpty()) {
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
        postEvent(Bus.UP_DOWNLOAD, true)
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
}