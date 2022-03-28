package io.legado.app.help.glide

import android.annotation.TargetApi
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.IntRange
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import io.legado.app.utils.stackBlur
import java.security.MessageDigest
import kotlin.math.min
import kotlin.math.roundToInt


/**
 * 模糊
 * @radius: 0..25
 */
class BlurTransformation(
    @IntRange(from = 0, to = 25) private val radius: Int
) : CenterCrop() {

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        val transform = super.transform(pool, toTransform, outWidth, outHeight)
        //图片缩小1/2
        val width = (min(outWidth, transform.width) / 5f).roundToInt()
        val height = (min(outHeight, transform.height) / 5f).roundToInt()
        val blurredBitmap = Bitmap.createScaledBitmap(transform, width, height, false)
        return blurredBitmap.stackBlur(radius)
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update("blur transformation".toByteArray())
    }
}
