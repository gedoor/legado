package io.legado.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.legado.app.service.help.ReadAloud

class OnUpgradeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            ReadAloud.stop(it)
        }
    }

}