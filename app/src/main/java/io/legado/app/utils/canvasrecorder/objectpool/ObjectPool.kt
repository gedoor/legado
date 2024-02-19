package io.legado.app.utils.canvasrecorder.objectpool

interface ObjectPool<T> {

    fun obtain(): T

    fun recycle(target: T)

    fun create(): T

}
