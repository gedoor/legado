package io.legado.app.utils

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.bumptech.glide.RequestManager

fun RequestManager.lifecycle(lifecycle: Lifecycle): RequestManager {

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
