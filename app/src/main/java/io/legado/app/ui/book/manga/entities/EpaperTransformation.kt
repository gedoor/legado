package io.legado.app.ui.book.manga.entities

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.annotation.IntRange
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest

/**
 * 墨水屏图片转换器。
 * 将彩色图片转换为灰度图，并可选择进行简单的二值化处理，以提高墨水屏显示效果。
 *
 * @param applyBinarization 是否应用二值化处理。墨水屏通常只有黑白两色，二值化可以增强对比度。
 *                          如果设置为false，则只进行灰度转换。
 * @param threshold 二值化的阈值（0-255）。低于此值的像素变为黑色，高于此值的像素变为白色。
 *                  仅当applyBinarization为true时有效。
 */
class EpaperTransformation(
    private val applyBinarization: Boolean = true,
    @IntRange(0, 255) private val threshold: Int = 128, // 默认阈值，可根据实际效果调整
) : BitmapTransformation() {

    private val ID =
        "io.legado.app.model.EpaperTransformation.${applyBinarization}.${threshold}"
    private val ID_BYTES = ID.toByteArray(CHARSET)

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int,
    ): Bitmap {
        val resultBitmap = pool.get(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        val paint = Paint()

        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f) // 将饱和度设置为0，实现灰度
        val filter = ColorMatrixColorFilter(colorMatrix)
        paint.colorFilter = filter
        canvas.drawBitmap(toTransform, 0f, 0f, paint)

        if (applyBinarization) {
            val pixels = IntArray(outWidth * outHeight)
            resultBitmap.getPixels(pixels, 0, outWidth, 0, 0, outWidth, outHeight)

            for (i in pixels.indices) {
                val pixel = pixels[i]
                val gray = Color.red(pixel)
                pixels[i] =
                    if (gray < threshold) Color.BLACK else Color.WHITE
            }
            resultBitmap.setPixels(pixels, 0, outWidth, 0, 0, outWidth, outHeight)
        }

        return resultBitmap
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID_BYTES)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EpaperTransformation

        if (applyBinarization != other.applyBinarization) return false
        if (threshold != other.threshold) return false
        if (ID != other.ID) return false // 确保ID也参与比较，因为ID包含了所有参数

        return true
    }

    override fun hashCode(): Int {
        var result = applyBinarization.hashCode()
        result = 31 * result + threshold
        result = 31 * result + ID.hashCode() // 确保ID也参与哈希计算
        return result
    }
}