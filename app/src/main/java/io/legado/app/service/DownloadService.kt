package io.legado.app.service

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import io.legado.app.BuildConfig
import io.legado.app.R
import io.legado.app.base.BaseService
import io.legado.app.constant.AppConst
import io.legado.app.constant.IntentAction
import io.legado.app.help.IntentHelp
import io.legado.app.utils.RealPathUtil
import io.legado.app.utils.msg
import org.jetbrains.anko.downloadManager
import org.jetbrains.anko.toast
import java.io.File


class DownloadService : BaseService() {

    private val downloads = hashMapOf<Long, String>()
    private val completeDownloads = hashSetOf<Long>()
    private val handler = Handler(Looper.getMainLooper())
    private val runnable = Runnable {
        checkDownloadState()
    }

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            queryState()
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(downloadReceiver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            IntentAction.start -> startDownload(
                intent.getLongExtra("downloadId", 0),
                intent.getStringExtra("fileName") ?: "未知文件"
            )
            IntentAction.play -> {
                val id = intent.getLongExtra("downloadId", 0)
                if (downloads[id]?.endsWith(".apk") == true) {
                    installApk(id)
                }
            }
            IntentAction.stop -> {
                val downloadId = intent.getLongExtra("downloadId", 0)
                downloads.remove(downloadId)
                stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startDownload(downloadId: Long, fileName: String) {
        if (downloadId > 0) {
            downloads[downloadId] = fileName
            queryState()
            checkDownloadState()
        }
    }

    private fun checkDownloadState() {
        handler.removeCallbacks(runnable)
        queryState()
        handler.postDelayed(runnable, 1000)
    }

    //查询下载进度
    private fun queryState() {
        val ids = downloads.keys
        val query = DownloadManager.Query()
        query.setFilterById(*ids.toLongArray())
        downloadManager.query(query).use { cursor ->
            if (!cursor.moveToFirst()) return
            val id = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID))
            val progress: Int =
                cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val max: Int =
                cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val status =
                when (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
                    DownloadManager.STATUS_PAUSED -> "暂停"
                    DownloadManager.STATUS_PENDING -> "待下载"
                    DownloadManager.STATUS_RUNNING -> "下载中"
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        if (!completeDownloads.contains(id)) {
                            completeDownloads.add(id)
                            if (downloads[id]?.endsWith(".apk") == true) {
                                installApk(id)
                            }
                        }
                        "下载完成"
                    }
                    DownloadManager.STATUS_FAILED -> "下载失败"
                    else -> "未知状态"
                }
            updateNotification(id, "${downloads[id]} $status", max, progress)
        }
    }

    private fun installApk(downloadId: Long) {
        downloadManager.getUriForDownloadedFile(downloadId)?.let {
            val filePath = RealPathUtil.getPath(this, it) ?: return
            val file = File(filePath)
            //调用系统安装apk
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {   //7.0版本以上
                val uriForFile: Uri =
                    FileProvider.getUriForFile(
                        this,
                        "${BuildConfig.APPLICATION_ID}.fileProvider",
                        file
                    )
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.setDataAndType(uriForFile, "application/vnd.android.package-archive")
            } else {
                val uri: Uri = Uri.fromFile(file)
                intent.setDataAndType(uri, "application/vnd.android.package-archive")
            }

            try {
                startActivity(intent)
            } catch (e: Exception) {
                toast(e.msg)
            }
        }
    }

    /**
     * 更新通知
     */
    private fun updateNotification(downloadId: Long, content: String, max: Int, progress: Int) {
        val notificationBuilder = NotificationCompat.Builder(this, AppConst.channelIdDownload)
            .setSmallIcon(R.drawable.ic_download)
            .setOngoing(true)
            .setContentTitle(getString(R.string.action_download))
        notificationBuilder.setContentIntent(
            IntentHelp.servicePendingIntent<DownloadService>(
                this,
                IntentAction.play,
                bundleOf("downloadId" to downloadId)
            )
        )
        notificationBuilder.addAction(
            R.drawable.ic_stop_black_24dp,
            getString(R.string.cancel),
            IntentHelp.servicePendingIntent<DownloadService>(
                this,
                IntentAction.stop,
                bundleOf("downloadId" to downloadId)
            )
        )
        notificationBuilder.setDeleteIntent(
            IntentHelp.servicePendingIntent<DownloadService>(
                this,
                IntentAction.stop,
                bundleOf("downloadId" to downloadId)
            )
        )
        notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        notificationBuilder.setContentText(content)
        notificationBuilder.setProgress(max, progress, false)
        notificationBuilder.setAutoCancel(true)
        val notification = notificationBuilder.build()
        startForeground(downloadId.toInt(), notification)
    }

}