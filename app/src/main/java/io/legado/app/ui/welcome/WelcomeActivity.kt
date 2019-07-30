package io.legado.app.ui.welcome

import android.animation.Animator
import android.animation.ValueAnimator
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.ui.main.MainActivity
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_welcome.*
import org.jetbrains.anko.startActivity

class WelcomeActivity : BaseActivity<AndroidViewModel>() {
    override val viewModel: AndroidViewModel
        get() = getViewModel(AndroidViewModel::class.java)
    override val layoutID: Int
        get() = R.layout.activity_welcome

    override fun onActivityCreated(viewModel: AndroidViewModel, savedInstanceState: Bundle?) {
        iv_bg.setColorFilter(ThemeStore.accentColor(this))
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