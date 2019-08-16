package io.legado.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter


class TimeElectricityReceiver : BroadcastReceiver() {

    companion object {

        fun register(context: Context): TimeElectricityReceiver {
            val receiver = TimeElectricityReceiver()
            val filter = IntentFilter()
            filter.addAction(Intent.ACTION_TIME_TICK)
            filter.addAction(Intent.ACTION_BATTERY_CHANGED)
            context.registerReceiver(receiver, filter)
            return receiver
        }

    }

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.action?.let {
            when (it) {
                Intent.ACTION_TIME_TICK -> {

                }
                Intent.ACTION_BATTERY_CHANGED -> {
                }
            }
        }
    }

}