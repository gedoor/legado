package io.legado.app.utils

import android.graphics.Canvas
import android.graphics.Bitmap
import android.graphics.drawable.PictureDrawable
import android.util.Size
import java.io.FileInputStream
import java.io.InputStream
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.PreserveAspectRatio
import com.caverock.androidsvg.SVGParseException
import kotlin.math.max

@Suppress("WeakerAccess", "MemberVisibilityCanBePrivate")
object SvgUtils {

    /**
     * 从Svg中解码bitmap
     * https://github.com/qoqa/glide-svg/blob/master/library/src/main/java/ch/qoqa/glide/svg/SvgBitmapTranscoder.kt
     */
    
    fun createBitmap(filePath: String, width: Int, height: Int? = null): Bitmap? {
        val inputStream = FileInputStream(filePath)
        return createBitmap(inputStream, width, height)
    }

    fun createBitmap(inputStream: InputStream, width: Int, height: Int? = null): Bitmap? {
        return kotlin.runCatching {
            val svg = SVG.getFromInputStream(inputStream)
            createBitmap(svg, width, height)
        }.getOrNull()
    }

    //获取svg图片大小
    fun getSize(filePath: String): Size? {
        val inputStream = FileInputStream(filePath)
        return getSize(inputStream)
    }

    fun getSize(inputStream: InputStream): Size? {
        return kotlin.runCatching {
            val svg = SVG.getFromInputStream(inputStream)
            getSize(svg)
        }.getOrNull()
    }

    /////// private method
    private fun createBitmap(svg: SVG, width: Int, height: Int? = null): Bitmap {
        val size = getSize(svg)
        val wRatio = width.let { size.width / it } ?: -1
        val hRatio = height?.let { size.height / it } ?: -1
        //如果超出指定大小，则缩小相应的比例
        val ratio = when {
            wRatio > 1 && hRatio > 1 -> max(wRatio, hRatio)
            wRatio > 1 -> wRatio
            hRatio > 1 -> hRatio
            else -> 1
        }
        svg.documentPreserveAspectRatio = PreserveAspectRatio.START
        val picture = svg.renderToPicture(size.width / ratio, size.height / ratio)
        val drawable = PictureDrawable(picture)

        val bitmap = Bitmap.createBitmap(size.width / ratio, size.height / ratio, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawPicture(drawable.picture)
        return bitmap
    }

    private fun getSize(svg: SVG): Size {
        val width = svg.documentWidth.toInt().takeIf { it > 0 }
            ?: (svg.documentViewBox.right - svg.documentViewBox.left).toInt()
        val height = svg.documentHeight.toInt().takeIf { it > 0 }
            ?: (svg.documentViewBox.bottom - svg.documentViewBox.top).toInt()
        return Size(width, height)      
    }

}
