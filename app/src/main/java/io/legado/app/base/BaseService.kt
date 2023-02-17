package io.legado.app.base

import android.content.Intent
import android.os.IBinder
import androidx.annotation.CallSuper
import androidx.lifecycle.LifecycleService
import io.legado.app.R
import io.legado.app.help.LifecycleHelp
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.lib.permission.Permissions
import io.legado.app.lib.permission.PermissionsCompat
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

abstract class BaseService : LifecycleService(), CoroutineScope by MainScope() {

    fun <T> execute(
        scope: CoroutineScope = this,
        context: CoroutineContext = Dispatchers.IO,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> T
    ) = Coroutine.async(scope, context, start) { block() }

    @CallSuper
    override fun onCreate() {
        super.onCreate()
        LifecycleHelp.onServiceCreate(this)
        upNotification()
        checkNotificationPermission()
    }

    @CallSuper
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        cancel()
        LifecycleHelp.onServiceDestroy(this)
    }

    /**
     * 更新通知
     */
    open fun upNotification() {

    }

    /**
     * 检测通知权限
     */
    private fun checkNotificationPermission()  {
        PermissionsCompat.Builder()
            .addPermissions(Permissions.POST_NOTIFICATIONS)
            .rationale(R.string.notification_permission_rationale)
            .onGranted {
                if (isActive) {
                    upNotification()
                }
            }
            .request()
    }
}