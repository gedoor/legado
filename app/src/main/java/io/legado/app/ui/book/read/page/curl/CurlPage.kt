package io.legado.app.ui.book.read.page.curl

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF

/**
 * Storage class for page textures, blend colors and possibly some other values
 * in the future.
 *
 * @author harism
 */
class CurlPage {

    private var mColorBack: Int = 0
    private var mColorFront: Int = 0
    private var mTextureBack: Bitmap? = null
    private var mTextureFront: Bitmap? = null
    /**
     * Returns true if textures have changed.
     */
    var texturesChanged: Boolean = false
        private set

    /**
     * Default constructor.
     */
    init {
        reset()
    }

    /**
     * Getter for color.
     */
    fun getColor(side: Int): Int {
        return when (side) {
            SIDE_FRONT -> mColorFront
            else -> mColorBack
        }
    }

    /**
     * Calculates the next highest power of two for a given integer.
     */
    private fun getNextHighestPO2(n: Int): Int {
        var n1 = n
        n1 -= 1
        n1 = n1 or (n1 shr 1)
        n1 = n1 or (n1 shr 2)
        n1 = n1 or (n1 shr 4)
        n1 = n1 or (n1 shr 8)
        n1 = n1 or (n1 shr 16)
        n1 = n1 or (n1 shr 32)
        return n1 + 1
    }

    /**
     * Generates nearest power of two sized Bitmap for give Bitmap. Returns this
     * new Bitmap using default return statement + original texture coordinates
     * are stored into RectF.
     */
    private fun getTexture(bitmap: Bitmap, textureRect: RectF): Bitmap {
        // Bitmap original size.
        val w = bitmap.width
        val h = bitmap.height
        // Bitmap size expanded to next power of two. This is done due to
        // the requirement on many devices, texture width and height should
        // be power of two.
        val newW = getNextHighestPO2(w)
        val newH = getNextHighestPO2(h)

        // TODO: Is there another way to create a bigger Bitmap and copy
        // original Bitmap to it more efficiently? Immutable bitmap anyone?
        val bitmapTex = Bitmap.createBitmap(newW, newH, bitmap.config)
        val c = Canvas(bitmapTex)
        c.drawBitmap(bitmap, 0f, 0f, null)

        // Calculate final texture coordinates.
        val texX = w.toFloat() / newW
        val texY = h.toFloat() / newH
        textureRect.set(0f, 0f, texX, texY)

        return bitmapTex
    }

    /**
     * Getter for textures. Creates Bitmap sized to nearest power of two, copies
     * original Bitmap into it and returns it. RectF given as parameter is
     * filled with actual texture coordinates in this new upscaled texture
     * Bitmap.
     */
    fun getTexture(textureRect: RectF, side: Int): Bitmap {
        return when (side) {
            SIDE_FRONT -> getTexture(mTextureFront!!, textureRect)
            else -> getTexture(mTextureBack!!, textureRect)
        }
    }

    /**
     * Returns true if back siding texture exists and it differs from front
     * facing one.
     */
    fun hasBackTexture(): Boolean {
        return mTextureFront != mTextureBack
    }

    /**
     * Recycles and frees underlying Bitmaps.
     */
    fun recycle() {
        if (mTextureFront != null) {
            mTextureFront!!.recycle()
        }
        mTextureFront = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)
        mTextureFront!!.eraseColor(mColorFront)
        if (mTextureBack != null) {
            mTextureBack!!.recycle()
        }
        mTextureBack = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)
        mTextureBack!!.eraseColor(mColorBack)
        texturesChanged = false
    }

    /**
     * Resets this CurlPage into its initial state.
     */
    fun reset() {
        mColorBack = Color.WHITE
        mColorFront = Color.WHITE
        recycle()
    }

    /**
     * Setter blend color.
     */
    fun setColor(color: Int, side: Int) {
        when (side) {
            SIDE_FRONT -> mColorFront = color
            SIDE_BACK -> mColorBack = color
            else -> {
                mColorBack = color
                mColorFront = mColorBack
            }
        }
    }

    /**
     * Setter for textures.
     */
    fun setTexture(texture: Bitmap?, side: Int) {
        var texture1 = texture
        if (texture1 == null) {
            texture1 = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)
            if (side == SIDE_BACK) {
                texture1!!.eraseColor(mColorBack)
            } else {
                texture1!!.eraseColor(mColorFront)
            }
        }
        when (side) {
            SIDE_FRONT -> {
                if (mTextureFront != null)
                    mTextureFront!!.recycle()
                mTextureFront = texture1
            }
            SIDE_BACK -> {
                if (mTextureBack != null)
                    mTextureBack!!.recycle()
                mTextureBack = texture1
            }
            SIDE_BOTH -> {
                if (mTextureFront != null)
                    mTextureFront!!.recycle()
                if (mTextureBack != null)
                    mTextureBack!!.recycle()
                mTextureBack = texture1
                mTextureFront = mTextureBack
            }
        }
        texturesChanged = true
    }

    companion object {

        const val SIDE_BACK = 2
        const val SIDE_BOTH = 3
        const val SIDE_FRONT = 1
    }

}
