package io.legado.app.ui.welcome

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.lib.theme.accentColor
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.main.MainActivity
import io.legado.app.utils.getPrefBoolean
import kotlinx.android.synthetic.main.activity_welcome.*
import org.jetbrains.anko.startActivity

open class WelcomeActivity : BaseActivity(R.layout.activity_welcome) {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        iv_bg.setColorFilter(accentColor)
        // 避免从桌面启动程序后，会重新实例化入口类的activity
        if (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0) {
            finish()
            return
        }
        val welAnimator = ValueAnimator.ofFloat(1f, 0f).setDuration(800)
        welAnimator.startDelay = 100
        welAnimator.addUpdateListener { animation ->
            val alpha = animation.animatedValue as Float
            iv_bg.alpha = alpha
        }
        welAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                startActivity<MainActivity>()
                if (getPrefBoolean(getString(R.string.pk_default_read))) {
                    startActivity<ReadBookActivity>()
                }
                finish()
            }

            override fun onAnimationEnd(animation: Animator) = Unit

            override fun onAnimationCancel(animation: Animator) = Unit

            override fun onAnimationRepeat(animation: Animator) = Unit
        })
        welAnimator.start()
    }

}

class Icon1Activity : WelcomeActivity()