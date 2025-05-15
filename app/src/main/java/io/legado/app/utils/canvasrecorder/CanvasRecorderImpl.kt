package io.legado.app.utils.canvasrecorder

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.bumptech.glide.Glide
import io.legado.app.utils.canvasrecorder.pools.CanvasPool
import splitties.init.appCtx

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
            bitmap = bitmapPool.get(width, height, Bitmap.Config.ARGB_8888)
        }
        if (bitmap!!.width != width || bitmap!!.height != height) {
            if (bitmap!!.isMutable && canReconfigure(width, height)) {
                bitmap!!.reconfigure(width, height, Bitmap.Config.ARGB_8888)
            } else {
                bitmapPool.put(bitmap!!)
                bitmap = bitmapPool.get(width, height, Bitmap.Config.ARGB_8888)
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
        bitmapPool.put(bitmap)
        this.bitmap = null
    }

    companion object {
        private val canvasPool = CanvasPool(2)
        private val bitmapPool = Glide.get(appCtx).bitmapPool
    }

}
