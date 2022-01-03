package io.legado.app.base.adapter

import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import io.legado.app.base.adapter.animations.*

/**
 * Created by Invincible on 2017/12/15.
 */
@Suppress("unused")
class ItemAnimation private constructor() {

    var itemAnimEnabled = false
    var itemAnimFirstOnly = true
    var itemAnimation: BaseAnimation? = null
    var itemAnimInterpolator: Interpolator = LinearInterpolator()
    var itemAnimDuration: Long = 300L
    var itemAnimStartPosition: Int = -1

    fun interpolator(interpolator: Interpolator) = apply {
        itemAnimInterpolator = interpolator
    }

    fun duration(duration: Long) = apply {
        itemAnimDuration = duration
    }

    fun startPosition(startPos: Int) = apply {
        itemAnimStartPosition = startPos
    }

    fun animation(animationType: Int = NONE, animation: BaseAnimation? = null) = apply {
        if (animation != null) {
            itemAnimation = animation
        } else {
            when (animationType) {
                FADE_IN -> itemAnimation = AlphaInAnimation()
                SCALE_IN -> itemAnimation = ScaleInAnimation()
                BOTTOM_SLIDE_IN -> itemAnimation = SlideInBottomAnimation()
                LEFT_SLIDE_IN -> itemAnimation = SlideInLeftAnimation()
                RIGHT_SLIDE_IN -> itemAnimation = SlideInRightAnimation()
            }
        }
    }

    fun enabled(enabled: Boolean) = apply {
        itemAnimEnabled = enabled
    }

    fun firstOnly(firstOnly: Boolean) = apply {
        itemAnimFirstOnly = firstOnly
    }

    companion object {
        const val NONE: Int = 0x00000000

        /**
         * Use with [.openLoadAnimation]
         */
        const val FADE_IN: Int = 0x00000001

        /**
         * Use with [.openLoadAnimation]
         */
        const val SCALE_IN: Int = 0x00000002

        /**
         * Use with [.openLoadAnimation]
         */
        const val BOTTOM_SLIDE_IN: Int = 0x00000003

        /**
         * Use with [.openLoadAnimation]
         */
        const val LEFT_SLIDE_IN: Int = 0x00000004

        /**
         * Use with [.openLoadAnimation]
         */
        const val RIGHT_SLIDE_IN: Int = 0x00000005

        fun create() = ItemAnimation()

    }
}