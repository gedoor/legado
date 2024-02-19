package io.legado.app.utils.canvasrecorder.pools

import android.graphics.Bitmap
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap

class BitmapPool {

    private val reusableBitmaps: MutableSet<SoftReference<Bitmap>> = ConcurrentHashMap.newKeySet()

    fun recycle(bitmap: Bitmap) {
        reusableBitmaps.add(SoftReference(bitmap))
    }

    fun obtain(width: Int, height: Int): Bitmap {
        if (reusableBitmaps.isEmpty()) {
            return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
        val iterator = reusableBitmaps.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next().get() ?: continue
            if (item.isMutable) {
                // Check to see it the item can be used for inBitmap.
                if (canReconfigure(item, width, height)) {
                    // Remove from reusable set so it can't be used again.
                    iterator.remove()
                    item.reconfigure(width, height, Bitmap.Config.ARGB_8888)
                    return item
                }
            } else {
                // Remove from the set if the reference has been cleared.
                iterator.remove()
            }
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    private fun canReconfigure(
        candidate: Bitmap,
        width: Int,
        height: Int
    ): Boolean {
        // From Android 4.4 (KitKat) onward we can re-use if the byte size of
        // the new bitmap is smaller than the reusable bitmap candidate
        // allocation byte count.
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
