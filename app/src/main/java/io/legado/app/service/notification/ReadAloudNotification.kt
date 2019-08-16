package io.legado.app.service.notification

import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import io.legado.app.R
import io.legado.app.constant.AppConst
import io.legado.app.help.PendingIntentHelp
import io.legado.app.service.ReadAloudService

object ReadAloudNotification {

    /**
     * 更新通知
     */
    fun upNotification(service: ReadAloudService) {
        var nTitle: String = when {
            service.pause -> service.getString(R.string.read_aloud_pause)
            service.timeMinute in 1..60 -> service.getString(R.string.read_aloud_timer, service.timeMinute)
            else -> service.getString(R.string.read_aloud_t)
        }
        nTitle += ": ${service.title}"
        var nSubtitle = service.subtitle
        if (service.subtitle.isEmpty())
            nSubtitle = service.getString(R.string.read_aloud_s)
        val builder = NotificationCompat.Builder(service, AppConst.channelIdReadAloud)
            .setSmallIcon(R.drawable.ic_volume_up)
            .setLargeIcon(BitmapFactory.decodeResource(service.resources, R.drawable.icon_read_book))
            .setOngoing(true)
            .setContentTitle(nTitle)
            .setContentText(nSubtitle)
            .setContentIntent(PendingIntentHelp.readBookActivityPendingIntent(service))
        if (service.pause) {
            builder.addAction(
                R.drawable.ic_play_24dp,
                service.getString(R.string.resume),
                PendingIntentHelp.aloudServicePendingIntent(service, "resume")
            )
        } else {
            builder.addAction(
                R.drawable.ic_pause_24dp,
                service.getString(R.string.pause),
                PendingIntentHelp.aloudServicePendingIntent(service, "pause")
            )
        }
        builder.addAction(
            R.drawable.ic_stop_black_24dp,
            service.getString(R.string.stop),
            PendingIntentHelp.aloudServicePendingIntent(service, "stop")
        )
        builder.addAction(
            R.drawable.ic_time_add_24dp,
            service.getString(R.string.set_timer),
            PendingIntentHelp.aloudServicePendingIntent(service, "setTimer")
        )
        builder.setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(service.mediaSessionCompat?.sessionToken)
                .setShowActionsInCompactView(0, 1, 2)
        )
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        val notification = builder.build()
        service.startForeground(112201, notification)
    }
}