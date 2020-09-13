package io.legado.app.service

import android.content.Intent
import androidx.core.app.NotificationCompat
import io.legado.app.R
import io.legado.app.base.BaseService
import io.legado.app.constant.AppConst
import io.legado.app.constant.IntentAction
import io.legado.app.help.IntentHelp

class DownloadService : BaseService() {

    private val notificationBuilder by lazy {
        val builder = NotificationCompat.Builder(this, AppConst.channelIdDownload)
            .setSmallIcon(R.drawable.ic_download)
            .setOngoing(true)
            .setContentTitle(getString(R.string.action_download))
        builder.addAction(
            R.drawable.ic_stop_black_24dp,
            getString(R.string.cancel),
            IntentHelp.servicePendingIntent<DownloadService>(this, IntentAction.stop)
        )
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    override fun onCreate() {
        super.onCreate()
        updateNotification("准备下载")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {

        }
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * 更新通知
     */
    private fun updateNotification(content: String) {
        notificationBuilder.setContentText(content)
        val notification = notificationBuilder.build()
        startForeground(AppConst.notificationIdDownload, notification)
    }
}