package io.legado.app.service

import android.content.Intent
import androidx.core.app.NotificationCompat
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseService
import io.legado.app.constant.AppConst
import io.legado.app.constant.IntentAction
import io.legado.app.help.AppConfig
import io.legado.app.help.IntentHelp
import io.legado.app.help.coroutine.CompositeCoroutine
import io.legado.app.service.help.CheckSource
import io.legado.app.ui.book.source.manage.BookSourceActivity
import kotlinx.coroutines.asCoroutineDispatcher
import org.jetbrains.anko.toast
import java.util.concurrent.Executors
import kotlin.math.min

class CheckSourceService : BaseService() {
    private var threadCount = AppConfig.threadCount
    private var searchPool = Executors.newFixedThreadPool(threadCount).asCoroutineDispatcher()
    private var tasks = CompositeCoroutine()
    private val allIds = ArrayList<String>()
    private val checkedIds = ArrayList<String>()
    private var processIndex = 0

    override fun onCreate() {
        super.onCreate()
        updateNotification(0, getString(R.string.start))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            IntentAction.start -> intent.getStringArrayListExtra("selectIds")?.let {
                check(it)
            }
            else -> stopSelf()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        tasks.clear()
        searchPool.close()
    }

    private fun check(ids: List<String>) {
        if (allIds.isNotEmpty()) {
            toast("已有书源在校验,等完成后再试")
            return
        }
        tasks.clear()
        allIds.clear()
        checkedIds.clear()
        allIds.addAll(ids)
        processIndex = 0
        threadCount = min(allIds.size, threadCount)
        updateNotification(0, getString(R.string.progress_show, 0, allIds.size))
        for (i in 0 until threadCount) {
            check()
        }
    }

    /**
     * 检测
     */
    private fun check() {
        val index = processIndex
        synchronized(this) {
            processIndex++
        }
        execute {
            if (index < allIds.size) {
                val sourceUrl = allIds[index]
                App.db.bookSourceDao().getBookSource(sourceUrl)?.let { source ->
                    if (source.searchUrl.isNullOrEmpty()) {
                        onNext(sourceUrl)
                    } else {
                        CheckSource(source).check(this, searchPool) {
                            onNext(it)
                        }
                    }
                } ?: onNext(sourceUrl)
            }
        }
    }

    private fun onNext(sourceUrl: String) {
        synchronized(this) {
            check()
            checkedIds.add(sourceUrl)
            updateNotification(
                checkedIds.size,
                getString(R.string.progress_show, checkedIds.size, allIds.size)
            )
            if (processIndex >= allIds.size + threadCount - 1) {
                stopSelf()
            }
        }
    }

    /**
     * 更新通知
     */
    private fun updateNotification(state: Int, msg: String) {
        val builder = NotificationCompat.Builder(this, AppConst.channelIdReadAloud)
            .setSmallIcon(R.drawable.ic_network_check)
            .setOngoing(true)
            .setContentTitle(getString(R.string.check_book_source))
            .setContentText(msg)
            .setContentIntent(
                IntentHelp.activityPendingIntent<BookSourceActivity>(this, "activity")
            )
            .addAction(
                R.drawable.ic_stop_black_24dp,
                getString(R.string.cancel),
                IntentHelp.servicePendingIntent<CheckSourceService>(this, IntentAction.stop)
            )
        builder.setProgress(allIds.size, state, false)
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        val notification = builder.build()
        startForeground(112202, notification)
    }

}