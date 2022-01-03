package io.legado.app.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context.POWER_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings


@Suppress("unused")
object SystemUtils {

    fun ignoreBatteryOptimization(activity: Activity) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) return

        val powerManager = activity.getSystemService(POWER_SERVICE) as PowerManager
        val hasIgnored = powerManager.isIgnoringBatteryOptimizations(activity.packageName)
        //  判断当前APP是否有加入电池优化的白名单，如果没有，弹出加入电池优化的白名单的设置对话框。
        if (!hasIgnored) {
            try {
                @SuppressLint("BatteryLife")
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:" + activity.packageName)
                activity.startActivity(intent)
            } catch (ignored: Throwable) {
            }

        }
    }

}
