package io.legado.app.service

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import io.legado.app.R
import io.legado.app.base.BaseService
import io.legado.app.constant.AppConst
import io.legado.app.constant.EventBus
import io.legado.app.constant.IntentAction
import io.legado.app.constant.PreferKey
import io.legado.app.help.IntentHelp
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.postEvent
import io.legado.app.web.HttpServer
import io.legado.app.web.WebSocketServer
import kotlinx.coroutines.launch
import org.jetbrains.anko.startService
import org.jetbrains.anko.toast
import java.io.IOException

class WebService : BaseService() {

    companion object {
        var isRun = false
        var hostAddress = ""

        fun start(context: Context) {
            context.startService<WebService>()
        }

        fun stop(context: Context) {
            if (isRun) {
                val intent = Intent(context, WebService::class.java)
                intent.action = IntentAction.stop
                context.startService(intent)
            }
        }

    }

    private var httpServer: HttpServer? = null
    private var webSocketServer: WebSocketServer? = null
    private var notificationContent = ""

    override fun onCreate() {
        super.onCreate()
        isRun = true
        notificationContent = getString(R.string.service_starting)
        upNotification()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRun = false
        if (httpServer?.isAlive == true) {
            httpServer?.stop()
        }
        if (webSocketServer?.isAlive == true) {
            webSocketServer?.stop()
        }
        postEvent(EventBus.WEB_SERVICE, "")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            IntentAction.stop -> stopSelf()
            else -> upWebServer()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun upWebServer() {
        if (httpServer?.isAlive == true) {
            httpServer?.stop()
        }
        if (webSocketServer?.isAlive == true) {
            webSocketServer?.stop()
        }
        val port = getPort()
        httpServer = HttpServer(port)
        webSocketServer = WebSocketServer(port + 1)
        val address = NetworkUtils.getLocalIPAddress()
        if (address != null) {
            try {
                httpServer?.start()
                webSocketServer?.start(1000 * 30) // 通信超时设置
                hostAddress = getString(R.string.http_ip, address.hostAddress, port)
                isRun = true
                postEvent(EventBus.WEB_SERVICE, hostAddress)
                notificationContent = hostAddress
                upNotification()
            } catch (e: IOException) {
                launch {
                    toast(e.localizedMessage ?: "")
                    stopSelf()
                }
            }
        } else {
            stopSelf()
        }
    }

    private fun getPort(): Int {
        var port = getPrefInt(PreferKey.webPort, 1122)
        if (port > 65530 || port < 1024) {
            port = 1122
        }
        return port
    }

    /**
     * 更新通知
     */
    private fun upNotification() {
        val builder = NotificationCompat.Builder(this, AppConst.channelIdWeb)
            .setSmallIcon(R.drawable.ic_web_service_noti)
            .setOngoing(true)
            .setContentTitle(getString(R.string.web_service))
            .setContentText(notificationContent)
        builder.addAction(
            R.drawable.ic_stop_black_24dp,
            getString(R.string.cancel),
            IntentHelp.servicePendingIntent<WebService>(this, IntentAction.stop)
        )
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        val notification = builder.build()
        startForeground(AppConst.notificationIdWeb, notification)
    }
}
