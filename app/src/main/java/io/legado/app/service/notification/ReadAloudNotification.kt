package io.legado.app.service.notification

import android.app.Service
import android.graphics.BitmapFactory
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import io.legado.app.R
import io.legado.app.constant.AppConst
import io.legado.app.help.PendingIntentHelp

object ReadAloudNotification {

    /**
     * 更新通知
     */
    fun upNotification(
        context: Service,
        mediaSessionCompat: MediaSessionCompat?,
        pause: Boolean,
        title: String,
        subtitle: String,
        timeMinute: Int = 0
    ) {
        var nTitle: String = when {
            pause -> context.getString(R.string.read_aloud_pause)
            timeMinute in 1..60 -> context.getString(R.string.read_aloud_timer, timeMinute)
            else -> context.getString(R.string.read_aloud_t)
        }
        nTitle += ": $title"
        var nSubtitle = subtitle
        if (subtitle.isEmpty())
            nSubtitle = context.getString(R.string.read_aloud_s)
        val builder = NotificationCompat.Builder(context, AppConst.channelIdReadAloud)
            .setSmallIcon(R.drawable.ic_volume_up)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.icon_read_book))
            .setOngoing(true)
            .setContentTitle(nTitle)
            .setContentText(nSubtitle)
            .setContentIntent(PendingIntentHelp.readBookActivityPendingIntent(context))
        if (pause) {
            builder.addAction(
                R.drawable.ic_play_24dp,
                context.getString(R.string.resume),
                PendingIntentHelp.aloudServicePendingIntent(context, "resume")
            )
        } else {
            builder.addAction(
                R.drawable.ic_pause_24dp,
                context.getString(R.string.pause),
                PendingIntentHelp.aloudServicePendingIntent(context, "pause")
            )
        }
        builder.addAction(
            R.drawable.ic_stop_black_24dp,
            context.getString(R.string.stop),
            PendingIntentHelp.aloudServicePendingIntent(context, "stop")
        )
        builder.addAction(
            R.drawable.ic_time_add_24dp,
            context.getString(R.string.set_timer),
            PendingIntentHelp.aloudServicePendingIntent(context, "setTimer")
        )
        builder.setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSessionCompat?.sessionToken)
                .setShowActionsInCompactView(0, 1, 2)
        )
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        val notification = builder.build()
        context.startForeground(112201, notification)
    }
}