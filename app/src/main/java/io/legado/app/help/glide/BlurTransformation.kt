package io.legado.app.help.glide

import android.graphics.Bitmap
import androidx.annotation.IntRange
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import io.legado.app.utils.stackBlur
import java.security.MessageDigest

/**
 * 模糊
 * @radius: 0..25
 */
class BlurTransformation(
    @IntRange(from = 0, to = 25) private val radius: Int
) : BitmapTransformation() {

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        return toTransform.stackBlur(radius)
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update("blur transformation".toByteArray())
    }
}
