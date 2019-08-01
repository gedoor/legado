package io.legado.app.ui.welcome

import android.animation.Animator
import android.animation.ValueAnimator
import android.os.Bundle
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.lib.theme.accentColor
import io.legado.app.ui.main.MainActivity
import kotlinx.android.synthetic.main.activity_welcome.*
import org.jetbrains.anko.startActivity

class WelcomeActivity : BaseActivity(R.layout.activity_welcome) {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        iv_bg.setColorFilter(accentColor)
        val welAnimator = ValueAnimator.ofFloat(1f, 0f).setDuration(800)
        welAnimator.startDelay = 100
        welAnimator.addUpdateListener { animation ->
            val alpha = animation.animatedValue as Float
            iv_bg.alpha = alpha
        }
        welAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                startActivity<MainActivity>()
                finish()
            }

            override fun onAnimationEnd(animation: Animator) {

            }

            override fun onAnimationCancel(animation: Animator) {

            }

            override fun onAnimationRepeat(animation: Animator) {

            }
        })
        welAnimator.start()
    }

}