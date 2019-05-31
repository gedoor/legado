package io.legado.app.help

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.*
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.transition.Transition
import java.io.File

object ImageLoader {

    fun load(context: Context, url: String?): ImageLoadBuilder<String> {
        return ImageLoadBuilder(context, url)
    }

    fun load(context: Context, @DrawableRes resId: Int?): ImageLoadBuilder<Int> {
        return ImageLoadBuilder(context, resId)
    }

    fun load(context: Context, file: File?): ImageLoadBuilder<File> {
        return ImageLoadBuilder(context, file)
    }

    fun load(context: Context, uri: Uri?): ImageLoadBuilder<Uri> {
        return ImageLoadBuilder(context, uri)
    }

    fun load(context: Context, drawable: Drawable?): ImageLoadBuilder<Drawable> {
        return ImageLoadBuilder(context, drawable)
    }

    fun load(context: Context, bitmap: Bitmap?): ImageLoadBuilder<Bitmap> {
        return ImageLoadBuilder(context, bitmap)
    }

    fun load(context: Context, bytes: ByteArray?): ImageLoadBuilder<ByteArray> {
        return ImageLoadBuilder(context, bytes)
    }


    fun with(context: Context): ImageLoadBuilder<Any> {
        return ImageLoadBuilder(context)
    }

    fun clear(imageView: ImageView) {
        with(imageView.context).clear(imageView)
    }

    class ImageLoadBuilder<S> constructor(context: Context, private var source: S? = null) {

        private val manager: RequestManager = Glide.with(context)
        private var requestOptions: RequestOptions = RequestOptions()
        private var crossFade: Boolean = false
        private var noCache: Boolean = false

        fun load(source: S): ImageLoadBuilder<S> {
            this.source = source
            return this
        }

        fun toRound(corner: Int): ImageLoadBuilder<S> {
            requestOptions = requestOptions.transform(RoundedCorners(corner))
            return this
        }

        fun toCropRound(corner: Int): ImageLoadBuilder<S> {
            requestOptions = requestOptions.transforms(CenterCrop(), RoundedCorners(corner))
            return this
        }

        fun toCircle(): ImageLoadBuilder<S> {
            requestOptions = requestOptions.transform(CircleCrop())
            return this
        }

        fun centerInside(): ImageLoadBuilder<S> {
            requestOptions = requestOptions.transform(CenterInside())
            return this
        }

        fun fitCenter(): ImageLoadBuilder<S> {
            requestOptions = requestOptions.transform(FitCenter())
            return this
        }

        fun centerCrop(): ImageLoadBuilder<S> {
            requestOptions = requestOptions.transform(CenterCrop())
            return this
        }

        fun crossFade(): ImageLoadBuilder<S> {
            crossFade = true
            return this
        }

        fun noCache(): ImageLoadBuilder<S> {
            noCache = true
            return this
        }

        fun placeholder(placeholder: Drawable): ImageLoadBuilder<S> {
            requestOptions = requestOptions.placeholder(placeholder)
            return this
        }

        fun placeholder(@DrawableRes resId: Int): ImageLoadBuilder<S> {
            requestOptions = requestOptions.placeholder(resId)
            return this
        }

        fun error(drawable: Drawable): ImageLoadBuilder<S> {
            requestOptions = requestOptions.error(drawable)
            return this
        }

        fun error(@DrawableRes resId: Int): ImageLoadBuilder<S> {
            requestOptions = requestOptions.error(resId)
            return this
        }

        fun override(width: Int, height: Int): ImageLoadBuilder<S> {
            requestOptions = requestOptions.override(width, height)
            return this
        }

        fun override(size: Int): ImageLoadBuilder<S> {
            requestOptions = requestOptions.override(size)
            return this
        }

        fun clear(imageView: ImageView) {
            manager.clear(imageView)
        }

        fun downloadOnly(target: ImageViewTarget<File>) {
            manager.downloadOnly().load(source).into(Target(target))
        }

        fun setAsDrawable(imageView: ImageView) {
            asDrawable().into(imageView)
        }

        fun setAsDrawable(target: ImageViewTarget<Drawable>) {
            asDrawable().into(Target(target))
        }

        fun setAsBitmap(imageView: ImageView) {
            asBitmap().into(imageView)
        }

        fun setAsBitmap(target: ImageViewTarget<Bitmap>) {
            asBitmap().into(Target(target))
        }

        fun setAsGif(imageView: ImageView) {
            asGif().into(imageView)
        }

        fun setAsGif(target: ImageViewTarget<GifDrawable>) {
            asGif().into(Target(target))
        }

        fun setAsFile(imageView: ImageView) {
            asFile().into(imageView)
        }

        fun setAsFile(target: ImageViewTarget<File>) {
            asFile().into(Target(target))
        }

        private fun asDrawable(): RequestBuilder<Drawable> {
            var builder: RequestBuilder<Drawable> = ensureOptions(manager.asDrawable().load(source))

            if (crossFade) {
                builder = builder.transition(DrawableTransitionOptions.withCrossFade())
            }

            return builder
        }

        private fun asBitmap(): RequestBuilder<Bitmap> {
            var builder: RequestBuilder<Bitmap> = ensureOptions(manager.asBitmap().load(source))

            if (crossFade) {
                builder = builder.transition(BitmapTransitionOptions.withCrossFade())
            }

            return builder
        }

        private fun asGif(): RequestBuilder<GifDrawable> {
            var builder: RequestBuilder<GifDrawable> = ensureOptions(manager.asGif().load(source))

            if (crossFade) {
                builder = builder.transition(DrawableTransitionOptions.withCrossFade())
            }

            return builder
        }

        private fun asFile(): RequestBuilder<File> {
            return manager.asFile().load(source)
        }

        private fun <ResourceType> ensureOptions(builder: RequestBuilder<ResourceType>): RequestBuilder<ResourceType> {
            return builder.apply(requestOptions.diskCacheStrategy(if (noCache) DiskCacheStrategy.NONE else DiskCacheStrategy.RESOURCE))
        }

        private inner class Target<R> constructor(private val target: ImageViewTarget<R>) :
            com.bumptech.glide.request.target.ImageViewTarget<R>(target.view) {

            init {
                if (this.target.waitForLayout) {
                    waitForLayout()
                }
            }

            override fun onResourceReady(resource: R, transition: Transition<in R>?) {
                if (!target.onResourceReady(resource)) {
                    super.onResourceReady(resource, transition)
                }
            }

            override fun setResource(resource: R?) {
                target.setResource(resource)
            }
        }
    }

    abstract class ImageViewTarget<R>(val view: ImageView) {
        internal var waitForLayout: Boolean = false

        fun waitForLayout(): ImageViewTarget<R> {
            waitForLayout = true
            return this
        }

        fun setResource(resource: R?) {

        }

        fun onResourceReady(resource: R?): Boolean {
            return false
        }

    }


}
