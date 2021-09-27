package io.legado.app.service

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
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
    private val downloads = hashMapOf<Long, Pair<String, String>>()
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
                intent.getStringExtra("url"),
                intent.getStringExtra("fileName")
            )
            IntentAction.play -> {
                val id = intent.getLongExtra("downloadId", 0)
                if (completeDownloads.contains(id)
                    && downloads[id]?.second?.endsWith(".apk") == true
                ) {
                    installApk(id)
                } else {
                    toastOnUi("下载的文件在Download文件夹")
                }
            }
            IntentAction.stop -> {
                val downloadId = intent.getLongExtra("downloadId", 0)
                removeDownload(downloadId)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @Synchronized
    private fun startDownload(url: String?, fileName: String?) {
        if (url == null || fileName == null) {
            if (downloads.isEmpty()) {
                stopSelf()
            }
            return
        }
        if (downloads.values.any { it.first == url }) {
            toastOnUi("已在下载列表")
            return
        }
        // 指定下载地址
        val request = DownloadManager.Request(Uri.parse(url))
        // 设置通知
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
        // 设置下载文件保存的路径和文件名
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        // 添加一个下载任务
        val downloadId = downloadManager.enqueue(request)
        downloads[downloadId] = Pair(url, fileName)
        queryState()
        if (upStateJob == null) {
            checkDownloadState()
        }
    }

    @Synchronized
    private fun removeDownload(downloadId: Long) {
        if (!completeDownloads.contains(downloadId)) {
            downloadManager.remove(downloadId)
        }
        downloads.remove(downloadId)
        completeDownloads.remove(downloadId)
        notificationManager.cancel(downloadId.toInt())
    }

    @Synchronized
    private fun successDownload(downloadId: Long) {
        if (!completeDownloads.contains(downloadId)) {
            completeDownloads.add(downloadId)
            val fileName = downloads[downloadId]?.second
            if (fileName?.endsWith(".apk") == true) {
                installApk(downloadId)
            } else {
                toastOnUi("$fileName ${getString(R.string.download_success)}")
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
    @Synchronized
    private fun queryState() {
        if (downloads.isEmpty()) {
            stopSelf()
            return
        }
        val ids = downloads.keys
        val query = DownloadManager.Query()
        query.setFilterById(*ids.toLongArray())
        downloadManager.query(query).use { cursor ->
            if (cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndex(DownloadManager.COLUMN_ID)
                val progressIndex =
                    cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val fileSizeIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                do {
                    val id = cursor.getLong(idIndex)
                    val progress = cursor.getInt(progressIndex)
                    val max = cursor.getInt(fileSizeIndex)
                    val status = when (cursor.getInt(statusIndex)) {
                        DownloadManager.STATUS_PAUSED -> getString(R.string.pause)
                        DownloadManager.STATUS_PENDING -> getString(R.string.wait_download)
                        DownloadManager.STATUS_RUNNING -> getString(R.string.downloading)
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            successDownload(id)
                            getString(R.string.download_success)
                        }
                        DownloadManager.STATUS_FAILED -> getString(R.string.download_error)
                        else -> getString(R.string.unknown_state)
                    }
                    upDownloadNotification(id, "${downloads[id]?.second} $status", max, progress)
                } while (cursor.moveToNext())
            }
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
        val notification = NotificationCompat.Builder(this, AppConst.channelIdDownload)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(getString(R.string.action_download))
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setOngoing(true)
            .build()
        startForeground(AppConst.notificationIdDownload, notification)
    }

    /**
     * 更新通知
     */
    private fun upDownloadNotification(downloadId: Long, content: String, max: Int, progress: Int) {
        val notification = NotificationCompat.Builder(this, AppConst.channelIdDownload)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(getString(R.string.action_download))
            .setContentIntent(
                servicePendingIntent<DownloadService>(IntentAction.play) {
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
            .build()
        notificationManager.notify(downloadId.toInt(), notification)
    }

}