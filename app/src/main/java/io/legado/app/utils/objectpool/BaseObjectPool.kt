package io.legado.app.utils.objectpool

import androidx.annotation.CallSuper
import androidx.core.util.Pools

abstract class BaseObjectPool<T : Any>(size: Int) : ObjectPool<T> {

    open val pool = Pools.SimplePool<T>(size)

    override fun obtain(): T {
        return pool.acquire() ?: create()
    }

    @CallSuper
    override fun recycle(target: T) {
        pool.release(target)
    }

}
