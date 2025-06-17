package io.legado.app.ui.book.manga.entities

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class GrayscaleTransformation : BitmapTransformation() {
    private val ID = "io.legado.app.model.GrayscaleTransformation"

    private val ID_BYTES = ID.toByteArray(StandardCharsets.UTF_8)

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int,
    ): Bitmap {
        val resultBitmap = pool.get(outWidth, outHeight, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(resultBitmap)
        val paint = Paint()

        val matrix = ColorMatrix(
            floatArrayOf(
                0.299f, 0.587f, 0.114f, 0f, 0f,
                0.299f, 0.587f, 0.114f, 0f, 0f,
                0.299f, 0.587f, 0.114f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val filter = ColorMatrixColorFilter(matrix)
        paint.colorFilter = filter
        canvas.drawBitmap(toTransform, 0f, 0f, paint)
        return resultBitmap
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID_BYTES)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as GrayscaleTransformation
        return ID == other.ID
    }

    override fun hashCode(): Int {
        return ID.hashCode()
    }
}
