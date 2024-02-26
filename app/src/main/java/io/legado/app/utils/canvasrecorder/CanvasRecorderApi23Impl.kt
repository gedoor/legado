package io.legado.app.utils.canvasrecorder

import android.graphics.Canvas
import android.graphics.Picture
import io.legado.app.utils.canvasrecorder.pools.PicturePool
import io.legado.app.utils.objectpool.synchronized

class CanvasRecorderApi23Impl : BaseCanvasRecorder() {

    private var picture: Picture? = null

    override val width get() = picture?.width ?: -1
    override val height get() = picture?.height ?: -1

    private fun initPicture() {
        if (picture == null) {
            picture = picturePool.obtain()
        }
    }

    override fun beginRecording(width: Int, height: Int): Canvas {
        initPicture()
        return picture!!.beginRecording(width, height)
    }

    override fun endRecording() {
        picture!!.endRecording()
        super.endRecording()
    }

    override fun draw(canvas: Canvas) {
        if (picture == null) return
        canvas.drawPicture(picture!!)
    }

    override fun recycle() {
        super.recycle()
        if (picture == null) return
        picturePool.recycle(picture!!)
        picture = null
    }

    companion object {
        private val picturePool = PicturePool().synchronized()
    }

}
