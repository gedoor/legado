package io.legado.app

import android.app.DownloadManager
import android.net.Uri
import android.os.Environment
import android.webkit.WebSettings
import android.webkit.WebView
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.runOnUI
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import splitties.init.appCtx
import splitties.systemservices.downloadManager

class HttpTest {

    @Test
    fun test() {
        webViewDownloadTest()
    }

    private fun webViewDownloadTest() {
        runOnUI {
            val webView = WebView(appCtx)
            val settings = webView.settings
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.blockNetworkImage = true
            settings.userAgentString = AppConfig.userAgent
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                print(url)
                webView.destroy()
            }
            webView.loadUrl("https://gj.legado.cc/legado/?url=https://miaogongzi.lanzout.com/iITmP0s7y26d&type=down")
        }
    }

    private fun downloadManagerTest() {
        runBlocking {
            val request =
                DownloadManager.Request(Uri.parse("https://gj.legado.cc/legado/?url=https://miaogongzi.lanzout.com/iITmP0s7y26d&type=down"))
            // 设置通知
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
            // 设置下载文件保存的路径和文件名
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "test.txt")
            // 添加一个下载任务
            val downloadId = downloadManager.enqueue(request)
            val query = DownloadManager.Query()
            query.setFilterById(downloadId)
            repeat(30) {
                downloadManager.query(query).use { cursor ->
                    if (cursor.moveToFirst()) {
                        val progressIndex =
                            cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val fileSizeIndex =
                            cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val progress = cursor.getInt(progressIndex)
                        val max = cursor.getInt(fileSizeIndex)
                        val status = when (cursor.getInt(statusIndex)) {
                            DownloadManager.STATUS_PAUSED -> appCtx.getString(R.string.pause)
                            DownloadManager.STATUS_PENDING -> appCtx.getString(R.string.wait_download)
                            DownloadManager.STATUS_RUNNING -> appCtx.getString(R.string.downloading)
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                appCtx.getString(R.string.download_success)
                            }

                            DownloadManager.STATUS_FAILED -> appCtx.getString(R.string.download_error)
                            else -> appCtx.getString(R.string.unknown_state)
                        }
                        print(status)
                        delay(1000)
                    } else {
                        return@runBlocking
                    }
                }
            }
        }
    }

}