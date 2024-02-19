package io.legado.app.utils.canvasrecorder.objectpool

fun <T> ObjectPool<T>.synchronized(): ObjectPool<T> = ObjectPoolLocked(this)
