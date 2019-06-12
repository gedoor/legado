package io.legado.app.ui.widget.anima.explosion_field

import android.animation.Animator
import android.view.View

interface OnAnimatorListener {
    fun onAnimationEnd(animator: Animator, view: View)
}
