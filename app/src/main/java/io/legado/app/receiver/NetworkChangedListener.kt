package io.legado.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import splitties.systemservices.connectivityManager

/**
 * 监测网络变化
 */
class NetworkChangedListener(private val context: Context) {

    var onNetworkChanged: (() -> Unit)? = null

    private val receiver: NetworkChangedReceiver? by lazy {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            NetworkChangedReceiver()
        }
        return@lazy null
    }

    fun register() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        onNetworkChanged?.invoke()
                    }
                }
            )
        } else {
            receiver?.let {
                context.registerReceiver(it, it.filter)
            }
        }
    }

    fun unRegister() {
        receiver?.let {
            context.unregisterReceiver(it)
        }
    }

    inner class NetworkChangedReceiver : BroadcastReceiver() {

        val filter = IntentFilter().apply {
            @Suppress("DEPRECATION")
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        }

        override fun onReceive(context: Context, intent: Intent) {
            onNetworkChanged?.invoke()
        }

    }

}