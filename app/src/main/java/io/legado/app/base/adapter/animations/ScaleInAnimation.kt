package io.legado.app.base.animations

import android.animation.Animator
import android.animation.ObjectAnimator
import android.view.View
import io.legado.app.base.adapter.animations.BaseAnimation


class ScaleInAnimation @JvmOverloads constructor(private val mFrom: Float = DEFAULT_SCALE_FROM) : BaseAnimation {

    override fun getAnimators(view: View): Array<Animator> {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", mFrom, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", mFrom, 1f)
        return arrayOf(scaleX, scaleY)
    }

    companion object {

        private const val DEFAULT_SCALE_FROM = .5f
    }
}
