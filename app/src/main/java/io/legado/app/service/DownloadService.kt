package io.legado.app.service

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import io.legado.app.R
import io.legado.app.base.BaseService
import io.legado.app.constant.AppConst
import io.legado.app.constant.IntentAction
import io.legado.app.utils.RealPathUtil
import io.legado.app.utils.msg
import io.legado.app.utils.servicePendingIntent
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import splitties.init.appCtx
import splitties.systemservices.downloadManager
import splitties.systemservices.notificationManager
import java.io.File


class DownloadService : BaseService() {
    private val groupKey = "${appCtx.packageName}.download"
    private val downloads = hashMapOf<Long, String>()
    private val completeDownloads = hashSetOf<Long>()
    private var upStateJob: Job? = null
    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            queryState()
        }
    }

    override fun onCreate() {
        super.onCreate()
        upSummaryNotification()
        registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
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
                if (completeDownloads.contains(id)
                    && downloads[id]?.endsWith(".apk") == true
                ) {
                    installApk(id)
                } else {
                    toastOnUi("下载的文件在Download文件夹")
                }
            }
            IntentAction.stop -> {
                val downloadId = intent.getLongExtra("downloadId", 0)
                downloads.remove(downloadId)
                notificationManager.cancel(downloadId.toInt())
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startDownload(downloadId: Long, fileName: String) {
        if (downloadId > 0) {
            downloads[downloadId] = fileName
            queryState()
            if (upStateJob == null) {
                checkDownloadState()
            }
        }
    }

    private fun checkDownloadState() {
        upStateJob?.cancel()
        upStateJob = launch {
            while (isActive) {
                queryState()
                delay(1000)
            }
        }
    }

    //查询下载进度
    private fun queryState() {
        if (downloads.isEmpty()) {
            stopSelf()
            return
        }
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
                val contentUrl = FileProvider.getUriForFile(this, AppConst.authority, file)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.setDataAndType(contentUrl, "application/vnd.android.package-archive")
            } else {
                val uri: Uri = Uri.fromFile(file)
                intent.setDataAndType(uri, "application/vnd.android.package-archive")
            }

            try {
                startActivity(intent)
            } catch (e: Exception) {
                toastOnUi(e.msg)
            }
        }
    }

    private fun upSummaryNotification() {
        val notificationBuilder = NotificationCompat.Builder(this, AppConst.channelIdDownload)
            .setSmallIcon(R.drawable.ic_download)
            .setOngoing(true)
            .setContentTitle(getString(R.string.action_download))
            .setGroup(groupKey)
            .setGroupSummary(true)
        val notification = notificationBuilder.build()
        startForeground(AppConst.notificationIdDownload, notification)
    }

    /**
     * 更新通知
     */
    private fun updateNotification(downloadId: Long, content: String, max: Int, progress: Int) {
        val notificationBuilder = NotificationCompat.Builder(this, AppConst.channelIdDownload)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(getString(R.string.action_download))
            .setContentIntent(
                servicePendingIntent<DownloadService>(IntentAction.play) {
                    putExtra("downloadId", downloadId)
                }
            )
            .addAction(
                R.drawable.ic_stop_black_24dp,
                getString(R.string.cancel),
                servicePendingIntent<DownloadService>(IntentAction.stop) {
                    putExtra("downloadId", downloadId)
                }
            )
            .setDeleteIntent(
                servicePendingIntent<DownloadService>(IntentAction.stop) {
                    putExtra("downloadId", downloadId)
                }
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentText(content)
            .setProgress(max, progress, false)
            .setGroup(groupKey)
        val notification = notificationBuilder.build()
        notificationManager.notify(downloadId.toInt(), notification)
    }

}