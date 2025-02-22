package io.legado.app.utils

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import splitties.init.appCtx

private val applicationRequestManager by lazy {
    Glide.with(appCtx)
}

fun RequestManager.lifecycle(lifecycle: Lifecycle): RequestManager {
    if (this === applicationRequestManager) {
        return this
    }

    val observer = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) = onStart()
        override fun onPause(owner: LifecycleOwner) = onStop()
        override fun onDestroy(owner: LifecycleOwner) {
            owner.lifecycle.removeObserver(this)
        }
    }

    lifecycle.addObserver(observer)

    return this
}
