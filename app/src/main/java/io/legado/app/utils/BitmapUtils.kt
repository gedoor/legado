package io.legado.app.utils

import android.content.Context
import android.graphics.*
import android.graphics.Bitmap.Config
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.view.View
import splitties.init.appCtx
import java.io.FileInputStream
import java.io.IOException
import kotlin.math.*


@Suppress("unused", "WeakerAccess", "MemberVisibilityCanBePrivate")
object BitmapUtils {

    /**
     * 从path中获取图片信息,在通过BitmapFactory.decodeFile(String path)方法将突破转成Bitmap时，
     * 遇到大一些的图片，我们经常会遇到OOM(Out Of Memory)的问题。所以用到了我们上面提到的BitmapFactory.Options这个类。
     *
     * @param path   文件路径
     * @param width  想要显示的图片的宽度
     * @param height 想要显示的图片的高度
     * @return
     */
    fun decodeBitmap(path: String, width: Int, height: Int): Bitmap? {
        val op = BitmapFactory.Options()
        op.inPreferredConfig = Config.RGB_565
        var ips = FileInputStream(path)
        // inJustDecodeBounds如果设置为true,仅仅返回图片实际的宽和高,宽和高是赋值给opts.outWidth,opts.outHeight;
        op.inJustDecodeBounds = true
        BitmapFactory.decodeStream(ips, null, op)
        //获取比例大小
        val wRatio = ceil((op.outWidth / width).toDouble()).toInt()
        val hRatio = ceil((op.outHeight / height).toDouble()).toInt()
        //如果超出指定大小，则缩小相应的比例
        if (wRatio > 1 && hRatio > 1) {
            if (wRatio > hRatio) {
                op.inSampleSize = wRatio
            } else {
                op.inSampleSize = hRatio
            }
        }
        op.inJustDecodeBounds = false
        ips = FileInputStream(path)
        return BitmapFactory.decodeStream(ips, null, op)
    }

    /** 从path中获取Bitmap图片
     * @param path 图片路径
     * @return
     */
    fun decodeBitmap(path: String): Bitmap? {

        val opts = BitmapFactory.Options()
        opts.inPreferredConfig = Config.RGB_565
        var ips = FileInputStream(path)
        opts.inJustDecodeBounds = true
        BitmapFactory.decodeStream(ips, null, opts)
        opts.inSampleSize = computeSampleSize(opts, -1, 128 * 128)
        opts.inJustDecodeBounds = false
        ips = FileInputStream(path)

        return BitmapFactory.decodeStream(ips, null, opts)
    }

    /**
     * 以最省内存的方式读取本地资源的图片
     * @param context 设备上下文
     * @param resId 资源ID
     * @return
     */
    fun decodeBitmap(context: Context, resId: Int): Bitmap? {
        val opt = BitmapFactory.Options()
        opt.inPreferredConfig = Config.RGB_565
        //获取资源图片
        val `is` = context.resources.openRawResource(resId)
        return BitmapFactory.decodeStream(`is`, null, opt)
    }

    /**
     * @param context 设备上下文
     * @param resId 资源ID
     * @param width
     * @param height
     * @return
     */
    fun decodeBitmap(context: Context, resId: Int, width: Int, height: Int): Bitmap? {

        var inputStream = context.resources.openRawResource(resId)

        val op = BitmapFactory.Options()
        // inJustDecodeBounds如果设置为true,仅仅返回图片实际的宽和高,宽和高是赋值给opts.outWidth,opts.outHeight;
        op.inJustDecodeBounds = true
        BitmapFactory.decodeStream(inputStream, null, op) //获取尺寸信息
        //获取比例大小
        val wRatio = ceil((op.outWidth / width).toDouble()).toInt()
        val hRatio = ceil((op.outHeight / height).toDouble()).toInt()
        //如果超出指定大小，则缩小相应的比例
        if (wRatio > 1 && hRatio > 1) {
            if (wRatio > hRatio) {
                op.inSampleSize = wRatio
            } else {
                op.inSampleSize = hRatio
            }
        }
        inputStream = context.resources.openRawResource(resId)
        op.inJustDecodeBounds = false
        return BitmapFactory.decodeStream(inputStream, null, op)
    }

    /**
     * @param context 设备上下文
     * @param fileNameInAssets Assets里面文件的名称
     * @param width 图片的宽度
     * @param height 图片的高度
     * @return Bitmap
     * @throws IOException
     */
    @Throws(IOException::class)
    fun decodeAssetsBitmap(
        context: Context,
        fileNameInAssets: String,
        width: Int,
        height: Int
    ): Bitmap? {
        var inputStream = context.assets.open(fileNameInAssets)
        val op = BitmapFactory.Options()
        // inJustDecodeBounds如果设置为true,仅仅返回图片实际的宽和高,宽和高是赋值给opts.outWidth,opts.outHeight;
        op.inJustDecodeBounds = true
        BitmapFactory.decodeStream(inputStream, null, op) //获取尺寸信息
        //获取比例大小
        val wRatio = ceil((op.outWidth / width).toDouble()).toInt()
        val hRatio = ceil((op.outHeight / height).toDouble()).toInt()
        //如果超出指定大小，则缩小相应的比例
        if (wRatio > 1 && hRatio > 1) {
            if (wRatio > hRatio) {
                op.inSampleSize = wRatio
            } else {
                op.inSampleSize = hRatio
            }
        }
        inputStream = context.assets.open(fileNameInAssets)
        op.inJustDecodeBounds = false
        return BitmapFactory.decodeStream(inputStream, null, op)
    }


    //图片不被压缩
    fun convertViewToBitmap(view: View, bitmapWidth: Int, bitmapHeight: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return bitmap
    }


    /**
     * @param options
     * @param minSideLength
     * @param maxNumOfPixels
     * @return
     * 设置恰当的inSampleSize是解决该问题的关键之一。BitmapFactory.Options提供了另一个成员inJustDecodeBounds。
     * 设置inJustDecodeBounds为true后，decodeFile并不分配空间，但可计算出原始图片的长度和宽度，即opts.width和opts.height。
     * 有了这两个参数，再通过一定的算法，即可得到一个恰当的inSampleSize。
     * 查看Android源码，Android提供了下面这种动态计算的方法。
     */
    fun computeSampleSize(
        options: BitmapFactory.Options,
        minSideLength: Int,
        maxNumOfPixels: Int
    ): Int {
        val initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels)
        var roundedSize: Int
        if (initialSize <= 8) {
            roundedSize = 1
            while (roundedSize < initialSize) {
                roundedSize = roundedSize shl 1
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8
        }
        return roundedSize
    }


    private fun computeInitialSampleSize(
        options: BitmapFactory.Options,
        minSideLength: Int,
        maxNumOfPixels: Int
    ): Int {

        val w = options.outWidth.toDouble()
        val h = options.outHeight.toDouble()

        val lowerBound = when (maxNumOfPixels) {
            -1 -> 1
            else -> ceil(sqrt(w * h / maxNumOfPixels)).toInt()
        }

        val upperBound = when (minSideLength) {
            -1 -> 128
            else -> min(
                floor(w / minSideLength),
                floor(h / minSideLength)
            ).toInt()
        }

        if (upperBound < lowerBound) {
            // return the larger one when there is no overlapping zone.
            return lowerBound
        }

        return when {
            maxNumOfPixels == -1 && minSideLength == -1 -> {
                1
            }
            minSideLength == -1 -> {
                lowerBound
            }
            else -> {
                upperBound
            }
        }
    }

    fun changeBitmapSize(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {

        val width = bitmap.width
        val height = bitmap.height

        //计算压缩的比率
        var scaleWidth = newWidth.toFloat() / width
        var scaleHeight = newHeight.toFloat() / height

        if (scaleWidth > scaleHeight) {
            scaleWidth = scaleHeight
        } else {
            scaleHeight = scaleWidth
        }

        //获取想要缩放的matrix
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)

        //获取新的bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)

    }

    /**
     * 高斯模糊
     */
    fun stackBlur(srcBitmap: Bitmap?): Bitmap? {
        if (srcBitmap == null) return null
        val rs = RenderScript.create(appCtx)
        val blurredBitmap = srcBitmap.copy(Config.ARGB_8888, true)

        //分配用于渲染脚本的内存
        val input = Allocation.createFromBitmap(
            rs,
            blurredBitmap,
            Allocation.MipmapControl.MIPMAP_FULL,
            Allocation.USAGE_SHARED
        )
        val output = Allocation.createTyped(rs, input.type)

        //加载我们想要使用的特定脚本的实例。
        val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        script.setInput(input)

        //设置模糊半径
        script.setRadius(8f)

        //启动 ScriptIntrinsicBlur
        script.forEach(output)

        //将输出复制到模糊的位图
        output.copyTo(blurredBitmap)

        return blurredBitmap
    }

    fun getMeanColor(bitmap: Bitmap): Int {
        val width: Int = bitmap.width
        val height: Int = bitmap.height
        var pixel: Int
        var pixelSumRed = 0
        var pixelSumBlue = 0
        var pixelSumGreen = 0
        for (i in 0..99) {
            for (j in 70..99) {
                pixel = bitmap.getPixel(
                    (i * width / 100.toFloat()).roundToInt(),
                    (j * height / 100.toFloat()).roundToInt()
                )
                pixelSumRed += Color.red(pixel)
                pixelSumGreen += Color.green(pixel)
                pixelSumBlue += Color.blue(pixel)
            }
        }
        val averagePixelRed = pixelSumRed / 3000
        val averagePixelBlue = pixelSumBlue / 3000
        val averagePixelGreen = pixelSumGreen / 3000
        return Color.rgb(
            averagePixelRed + 3,
            averagePixelGreen + 3,
            averagePixelBlue + 3
        )
    }

}
