package io.legado.app.service

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import io.legado.app.R
import io.legado.app.base.BaseService
import io.legado.app.constant.Action
import io.legado.app.constant.AppConst
import io.legado.app.help.IntentHelp
import org.jetbrains.anko.startService

class WebService : BaseService() {

    companion object {
        var isRun = false

        fun start(context: Context) {
            context.startService<WebService>()
        }

        fun stop(context: Context) {
            if (isRun) {
                val intent = Intent(context, WebService::class.java)
                intent.action = Action.stop
                context.startService(intent)
            }
        }

    }

    override fun onCreate() {
        super.onCreate()
        isRun = true
        updateNotification(getString(R.string.service_starting))
    }

    override fun onDestroy() {
        super.onDestroy()
        isRun = false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Action.stop -> stopSelf()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * 更新通知
     */
    private fun updateNotification(content: String) {
        val builder = NotificationCompat.Builder(this, AppConst.channelIdWeb)
            .setSmallIcon(R.drawable.ic_web_service_noti)
            .setOngoing(true)
            .setContentTitle(getString(R.string.web_service))
            .setContentText(content)
        builder.addAction(
            R.drawable.ic_stop_black_24dp,
            getString(R.string.cancel),
            IntentHelp.servicePendingIntent<WebService>(this, Action.stop)
        )
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        val notification = builder.build()
        startForeground(AppConst.notificationIdWeb, notification)
    }
}