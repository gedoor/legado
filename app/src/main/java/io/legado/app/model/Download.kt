package io.legado.app.model

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import io.legado.app.constant.IntentAction
import io.legado.app.service.DownloadService
import io.legado.app.utils.startService
import splitties.systemservices.downloadManager

object Download {


    fun start(context: Context, url: String, fileName: String) {
        // 指定下载地址
        val request = DownloadManager.Request(Uri.parse(url))
        // 设置通知
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
        // 设置下载文件保存的路径和文件名
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        // 添加一个下载任务
        val downloadId = downloadManager.enqueue(request)
        context.startService<DownloadService> {
            action = IntentAction.start
            putExtra("downloadId", downloadId)
            putExtra("fileName", fileName)
        }
    }

}