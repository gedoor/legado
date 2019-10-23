package io.legado.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import io.legado.app.constant.Bus
import io.legado.app.utils.postEvent


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
                    postEvent(Bus.TIME_CHANGED, "")
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    postEvent(Bus.BATTERY_CHANGED, level)
                }
            }
        }
    }

}