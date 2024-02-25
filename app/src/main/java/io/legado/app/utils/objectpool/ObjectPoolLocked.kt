package io.legado.app.utils.objectpool

class ObjectPoolLocked<T>(private val delegate: ObjectPool<T>) : ObjectPool<T> by delegate {

    @Synchronized
    override fun obtain(): T {
        return delegate.obtain()
    }

    @Synchronized
    override fun recycle(target: T) {
        return delegate.recycle(target)
    }

}
