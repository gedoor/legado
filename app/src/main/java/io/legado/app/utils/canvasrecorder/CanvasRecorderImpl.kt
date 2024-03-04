package io.legado.app.utils.canvasrecorder

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import io.legado.app.utils.canvasrecorder.pools.BitmapPool
import io.legado.app.utils.canvasrecorder.pools.CanvasPool

class CanvasRecorderImpl : BaseCanvasRecorder() {

    var bitmap: Bitmap? = null
    var canvas: Canvas? = null

    override val width get() = bitmap?.width ?: -1
    override val height get() = bitmap?.height ?: -1

    private fun init(width: Int, height: Int) {
        if (width <= 0 || height <= 0) {
            return
        }
        if (bitmap == null) {
            bitmap = BitmapPool.obtain(width, height)
        }
        if (bitmap!!.width != width || bitmap!!.height != height) {
            if (canReconfigure(width, height)) {
                bitmap!!.reconfigure(width, height, Bitmap.Config.ARGB_8888)
            } else {
                BitmapPool.recycle(bitmap!!)
                bitmap = BitmapPool.obtain(width, height)
            }
        }
    }

    private fun canReconfigure(width: Int, height: Int): Boolean {
        return bitmap!!.allocationByteCount >= width * height * 4
    }

    override fun beginRecording(width: Int, height: Int): Canvas {
        init(width, height)
        bitmap?.eraseColor(Color.TRANSPARENT)
        canvas = canvasPool.obtain().apply { setBitmap(bitmap) }
        return canvas!!
    }

    override fun endRecording() {
        bitmap?.prepareToDraw()
        super.endRecording()
        canvasPool.recycle(canvas!!)
        canvas = null
    }

    override fun draw(canvas: Canvas) {
        if (bitmap == null) return
        canvas.drawBitmap(bitmap!!, 0f, 0f, null)
    }

    override fun recycle() {
        super.recycle()
        val bitmap = bitmap ?: return
        BitmapPool.recycle(bitmap)
        this.bitmap = null
    }

    companion object {
        private val canvasPool = CanvasPool(2)
    }

}
