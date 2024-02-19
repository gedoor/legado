package io.legado.app.utils.canvasrecorder.objectpool

import androidx.annotation.CallSuper
import java.lang.ref.SoftReference
import java.util.LinkedList

abstract class BaseObjectPool<T> : ObjectPool<T> {

    private val pool = LinkedList<SoftReference<T>>()

    override fun obtain(): T {
        while (true) {
            if (pool.isEmpty()) break
            return pool.poll()?.get() ?: continue
        }
        return create()
    }

    @CallSuper
    override fun recycle(target: T) {
        pool.add(SoftReference(target))
    }

}
