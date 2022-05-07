package io.legado.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager

/**
 * 监测网络变化
 */
class NetworkChangedReceiver : BroadcastReceiver() {

    var onReceiver: ((context: Context, intent: Intent) -> Unit)? = null

    val filter = IntentFilter().apply {
        @Suppress("DEPRECATION")
        addAction(ConnectivityManager.CONNECTIVITY_ACTION)
    }

    override fun onReceive(context: Context, intent: Intent) {
        onReceiver?.invoke(context, intent)
    }

}