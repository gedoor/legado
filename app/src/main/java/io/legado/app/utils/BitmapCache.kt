package io.legado.app.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap

object BitmapCache {

    private val reusableBitmaps: MutableSet<SoftReference<Bitmap>> = ConcurrentHashMap.newKeySet()

    fun add(bitmap: Bitmap) {
        reusableBitmaps.add(SoftReference(bitmap))
        trimSize()
    }

    fun clear() {
        if (reusableBitmaps.isEmpty()) {
            return
        }
        val iterator = reusableBitmaps.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next().get() ?: continue
            item.recycle()
            iterator.remove()
        }
    }

    private fun trimSize() {
        var byteCount = 0
        val iterator = reusableBitmaps.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next().get() ?: continue
            if (byteCount > 128 * 1024 * 1024) {
                item.recycle()
                iterator.remove()
            } else {
                byteCount += item.byteCount
            }
        }
    }

    fun addInBitmapOptions(options: BitmapFactory.Options) {
        // inBitmap only works with mutable bitmaps, so force the decoder to
        // return mutable bitmaps.
        options.inMutable = true

        // Try to find a bitmap to use for inBitmap.
        getBitmapFromReusableSet(options)?.also { inBitmap ->
            // If a suitable bitmap has been found, set it as the value of
            // inBitmap.
            options.inBitmap = inBitmap
        }
    }


    private fun getBitmapFromReusableSet(options: BitmapFactory.Options): Bitmap? {
        if (reusableBitmaps.isEmpty()) {
            return null
        }
        val iterator = reusableBitmaps.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next().get() ?: continue
            if (item.isMutable) {
                // Check to see it the item can be used for inBitmap.
                if (canUseForInBitmap(item, options)) {
                    // Remove from reusable set so it can't be used again.
                    iterator.remove()
                    return item
                }
            } else {
                // Remove from the set if the reference has been cleared.
                iterator.remove()
            }
        }
        return null
    }

    private fun canUseForInBitmap(
        candidate: Bitmap,
        targetOptions: BitmapFactory.Options
    ): Boolean {
        // From Android 4.4 (KitKat) onward we can re-use if the byte size of
        // the new bitmap is smaller than the reusable bitmap candidate
        // allocation byte count.
        val width: Int = targetOptions.outWidth / targetOptions.inSampleSize
        val height: Int = targetOptions.outHeight / targetOptions.inSampleSize
        val byteCount: Int = width * height * getBytesPerPixel(candidate.config)
        return byteCount <= candidate.allocationByteCount
    }

    /**
     * A helper function to return the byte usage per pixel of a bitmap based on its configuration.
     */
    private fun getBytesPerPixel(config: Bitmap.Config): Int {
        return when (config) {
            Bitmap.Config.ARGB_8888 -> 4
            Bitmap.Config.RGB_565, Bitmap.Config.ARGB_4444 -> 2
            Bitmap.Config.ALPHA_8 -> 1
            else -> 1
        }
    }

}
