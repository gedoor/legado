package io.legado.app.utils

import android.graphics.Canvas
import android.graphics.Picture
import android.os.Build
import androidx.core.graphics.record

class PictureMirror {

    var picture: Picture? = null
    var isDirty = true

    inline fun draw(canvas: Canvas, width: Int, height: Int, block: Canvas.() -> Unit) {
        if (atLeastApi23) {
            if (picture == null) picture = Picture()
            val picture = picture!!
            if (isDirty) {
                isDirty = false
                picture.record(width, height, block)
            }
            canvas.drawPicture(picture)
        } else {
            canvas.block()
        }
    }

    fun invalidate() {
        isDirty = true
    }

    fun recycle() {
        picture = null
        isDirty = true
    }

    companion object {
        val atLeastApi23 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

}
