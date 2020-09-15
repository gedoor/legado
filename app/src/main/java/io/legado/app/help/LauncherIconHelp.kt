package io.legado.app.help

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import io.legado.app.App
import io.legado.app.R
import io.legado.app.ui.welcome.*
import org.jetbrains.anko.toast

/**
 * Created by GKF on 2018/2/27.
 * 更换图标
 */
object LauncherIconHelp {
    private val packageManager: PackageManager = App.INSTANCE.packageManager
    private val componentNames = arrayListOf(
        ComponentName(App.INSTANCE, Launcher1::class.java.name),
        ComponentName(App.INSTANCE, Launcher2::class.java.name),
        ComponentName(App.INSTANCE, Launcher3::class.java.name),
        ComponentName(App.INSTANCE, Launcher4::class.java.name),
        ComponentName(App.INSTANCE, Launcher5::class.java.name),
        ComponentName(App.INSTANCE, Launcher6::class.java.name)
    )

    fun changeIcon(icon: String?) {
        if (icon.isNullOrEmpty()) return
        if (Build.VERSION.SDK_INT < 26) {
            App.INSTANCE.toast(R.string.change_icon_error)
            return
        }
        var hasEnabled = false
        componentNames.forEach {
            if (icon.equals(it.className.substringAfterLast("."), true)) {
                hasEnabled = true
                //启用
                packageManager.setComponentEnabledSetting(
                    it,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            } else {
                //禁用
                packageManager.setComponentEnabledSetting(
                    it,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
        if (hasEnabled) {
            packageManager.setComponentEnabledSetting(
                ComponentName(App.INSTANCE, WelcomeActivity::class.java.name),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        } else {
            packageManager.setComponentEnabledSetting(
                ComponentName(App.INSTANCE, WelcomeActivity::class.java.name),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}