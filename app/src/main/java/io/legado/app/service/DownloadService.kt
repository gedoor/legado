package io.legado.app.service

import android.content.Intent
import androidx.core.app.NotificationCompat
import io.legado.app.R
import io.legado.app.base.BaseService
import io.legado.app.constant.Action
import io.legado.app.constant.AppConst
import io.legado.app.help.IntentHelp
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

class DownloadService : BaseService() {
    private var searchPool = Executors.newFixedThreadPool(16).asCoroutineDispatcher()

    override fun onCreate() {
        super.onCreate()
        updateNotification("正在启动下载")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                Action.start -> download(
                    intent.getStringExtra("bookUrl"),
                    intent.getIntExtra("start", 0),
                    intent.getIntExtra("end", 0)
                )
                Action.stop -> stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        searchPool.close()
    }

    private fun download(bookUrl: String?, start: Int, end: Int) {
        if (bookUrl == null) return

    }

    /**
     * 更新通知
     */
    private fun updateNotification(content: String) {
        val builder = NotificationCompat.Builder(this, AppConst.channelIdDownload)
            .setSmallIcon(R.drawable.ic_download)
            .setOngoing(true)
            .setContentTitle(getString(R.string.download_offline))
            .setContentText(content)
        builder.addAction(
            R.drawable.ic_stop_black_24dp,
            getString(R.string.cancel),
            IntentHelp.servicePendingIntent<WebService>(this, Action.stop)
        )
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        val notification = builder.build()
        startForeground(AppConst.notificationIdDownload, notification)
    }
}