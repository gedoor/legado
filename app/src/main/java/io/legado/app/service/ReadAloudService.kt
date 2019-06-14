package io.legado.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class ReadAloudService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

}