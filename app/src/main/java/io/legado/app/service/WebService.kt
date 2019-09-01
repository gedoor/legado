package io.legado.app.service

import android.content.Context
import android.content.Intent
import io.legado.app.base.BaseService
import io.legado.app.constant.Action
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
}