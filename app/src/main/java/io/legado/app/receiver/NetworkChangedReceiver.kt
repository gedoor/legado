package io.legado.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * 监测网络变化
 */
class NetworkChangedReceiver : BroadcastReceiver() {

    val filter = IntentFilter().apply {
        addAction("android.net.conn.CONNECTIVITY_CHANGE")
        addAction("android.net.wifi.WIFI_STATE_CHANGED")
        addAction("android.net.wifi.STATE_CHANGE")
    }

    override fun onReceive(context: Context?, intent: Intent?) {

    }

}