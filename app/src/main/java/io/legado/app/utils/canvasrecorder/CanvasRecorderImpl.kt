package io.legado.app.utils.canvasrecorder

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color

class CanvasRecorderImpl : BaseCanvasRecorder() {

    var bitmap: Bitmap? = null
    var canvas: Canvas? = null

    override val width get() = bitmap?.width ?: -1
    override val height get() = bitmap?.height ?: -1

    private fun init(width: Int, height: Int) {
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
        if (canvas == null) {
            canvas = Canvas(bitmap!!)
        }
        if (bitmap!!.width != width || bitmap!!.height != height) {
            if (canReconfigure(width, height)) {
                bitmap!!.reconfigure(width, height, Bitmap.Config.ARGB_8888)
            } else {
                bitmap!!.recycle()
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            }
            canvas!!.setBitmap(bitmap!!)
        }
    }

    private fun canReconfigure(width: Int, height: Int): Boolean {
        return bitmap!!.allocationByteCount >= width * height * 4
    }

    override fun beginRecording(width: Int, height: Int): Canvas {
        init(width, height)
        bitmap!!.eraseColor(Color.TRANSPARENT)
        return canvas!!
    }

    override fun endRecording() {
        bitmap!!.prepareToDraw()
        super.endRecording()
    }

    override fun draw(canvas: Canvas) {
        if (bitmap == null) return
        canvas.drawBitmap(bitmap!!, 0f, 0f, null)
    }

    override fun recycle() {
        super.recycle()
        bitmap?.recycle()
        bitmap = null
    }

}
