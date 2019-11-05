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
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation

import java.security.MessageDigest

/**
 * 模糊
 */
class BlurTransformation(val context: Context, private val radius: Int) : BitmapTransformation() {

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        val blurredBitmap = toTransform.copy(Bitmap.Config.ARGB_8888, true)
        val rs: RenderScript = RenderScript.create(context)
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
        //设置模糊半径
        script.setRadius(radius.toFloat())

        // Start the ScriptIntrinsicBlur
        //启动 ScriptIntrinsicBlur,
        script.forEach(output)

        // Copy the output to the blurred bitmap
        //将输出复制到模糊的位图
        output.copyTo(blurredBitmap)
        rs.destroy()

        return blurredBitmap
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update("blur transformation".toByteArray())
    }
}
