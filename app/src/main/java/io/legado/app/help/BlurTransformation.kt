package io.legado.app.help

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import java.security.MessageDigest
import kotlin.math.min
import kotlin.math.roundToInt


/**
 * 模糊
 * @radius: 0..25
 */
class BlurTransformation(context: Context, private val radius: Int) : CenterCrop() {
    private val rs: RenderScript = RenderScript.create(context)

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        val transform = super.transform(pool, toTransform, outWidth, outHeight)
        //图片缩小1/2
        val width = (min(outWidth, transform.width) / 2f).roundToInt()
        val height = (min(outHeight, transform.height) / 2f).roundToInt()
        val blurredBitmap = Bitmap.createScaledBitmap(transform, width, height, false)
        // Allocate memory for Renderscript to work with
        //分配用于渲染脚本的内存
        val input = Allocation.createFromBitmap(
            rs,
            blurredBitmap,
            Allocation.MipmapControl.MIPMAP_FULL,
            Allocation.USAGE_SHARED
        )
        val output = Allocation.createTyped(rs, input.type)

        // Load up an instance of the specific script that we want to use.
        //加载我们想要使用的特定脚本的实例。
        val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        script.setInput(input)

        // Set the blur radius
        //设置模糊半径0..25
        script.setRadius(radius.toFloat())

        // Start the ScriptIntrinsicBlur
        //启动 ScriptIntrinsicBlur,
        script.forEach(output)

        // Copy the output to the blurred bitmap
        //将输出复制到模糊的位图
        output.copyTo(blurredBitmap)

        return blurredBitmap
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update("blur transformation".toByteArray())
    }
}
