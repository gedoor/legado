package io.legado.app.utils.canvasrecorder.pools

import android.graphics.Canvas
import androidx.core.util.Pools

class CanvasPool(size: Int) {

    private val pool = Pools.SynchronizedPool<Canvas>(size)

    fun obtain(): Canvas {
        val canvas = pool.acquire() ?: Canvas()
        return canvas
    }

    fun recycle(canvas: Canvas) {
        canvas.setBitmap(null)
        canvas.restoreToCount(1)
        pool.release(canvas)
    }

}
