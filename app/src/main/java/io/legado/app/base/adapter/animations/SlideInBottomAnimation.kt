package io.legado.app.base.adapter.animations

import android.animation.Animator
import android.animation.ObjectAnimator
import android.view.View

class SlideInBottomAnimation : BaseAnimation {


    override fun getAnimators(view: View): Array<Animator> =
        arrayOf(ObjectAnimator.ofFloat(view, "translationY", view.measuredHeight.toFloat(), 0f))
}
