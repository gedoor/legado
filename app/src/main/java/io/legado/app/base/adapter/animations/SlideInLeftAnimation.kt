package io.legado.app.base.adapter.animations

import android.animation.Animator
import android.animation.ObjectAnimator
import android.view.View
import io.legado.app.base.adapter.animations.BaseAnimation


class SlideInLeftAnimation : BaseAnimation {


    override fun getAnimators(view: View): Array<Animator> =
            arrayOf(ObjectAnimator.ofFloat(view, "translationX", -view.rootView.width.toFloat(), 0f))
}
