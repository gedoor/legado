package io.legado.app.help.glide

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.DrawableRes
import com.bumptech.glide.RequestBuilder
import io.legado.app.constant.AppPattern.dataUriRegex
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.utils.isAbsUrl
import io.legado.app.utils.isContentScheme
import java.io.File

@Suppress("unused")
object ImageLoader {

    /**
     * 自动判断path类型
     */
    fun load(context: Context, path: String?): RequestBuilder<Drawable> {
        return when {
            path.isNullOrEmpty() -> GlideApp.with(context).load(path)
            dataUriRegex.find(path) != null -> {
                //glide内部已经实现dataUri解析
                GlideApp.with(context).load(path)
            }
            path.isAbsUrl() -> GlideApp.with(context).load(path)
            path.isContentScheme() -> GlideApp.with(context).load(Uri.parse(path))
            else -> kotlin.runCatching {
                val file = File(path)
                if (file.exists()) {
                    GlideApp.with(context).load(file)
                } else {
                    GlideApp.with(context).load(path)
                }
            }.getOrElse {
                GlideApp.with(context).load(path)
            }
        }
    }

    fun loadBitmap(context: Context, path: String?): RequestBuilder<Bitmap> {
        return when {
            path.isNullOrEmpty() -> GlideApp.with(context).asBitmap().load(path)
            path.isAbsUrl() -> GlideApp.with(context).asBitmap()
                .load(AnalyzeUrl(path).getGlideUrl())
            path.isContentScheme() -> GlideApp.with(context).asBitmap().load(Uri.parse(path))
            else -> kotlin.runCatching {
                GlideApp.with(context).asBitmap().load(File(path))
            }.getOrElse {
                GlideApp.with(context).asBitmap().load(path)
            }
        }
    }

    fun loadFile(context: Context, path: String?): RequestBuilder<File> {
        return when {
            path.isNullOrEmpty() -> GlideApp.with(context).asFile().load(path)
            path.isAbsUrl() -> GlideApp.with(context).asFile().load(AnalyzeUrl(path).getGlideUrl())
            path.isContentScheme() -> GlideApp.with(context).asFile().load(Uri.parse(path))
            else -> kotlin.runCatching {
                GlideApp.with(context).asFile().load(File(path))
            }.getOrElse {
                GlideApp.with(context).asFile().load(path)
            }
        }
    }

    fun load(context: Context, @DrawableRes resId: Int?): RequestBuilder<Drawable> {
        return GlideApp.with(context).load(resId)
    }

    fun load(context: Context, file: File?): RequestBuilder<Drawable> {
        return GlideApp.with(context).load(file)
    }

    fun load(context: Context, uri: Uri?): RequestBuilder<Drawable> {
        return GlideApp.with(context).load(uri)
    }

    fun load(context: Context, drawable: Drawable?): RequestBuilder<Drawable> {
        return GlideApp.with(context).load(drawable)
    }

    fun load(context: Context, bitmap: Bitmap?): RequestBuilder<Drawable> {
        return GlideApp.with(context).load(bitmap)
    }

    fun load(context: Context, bytes: ByteArray?): RequestBuilder<Drawable> {
        return GlideApp.with(context).load(bytes)
    }

}
