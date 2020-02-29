package io.legado.app.ui.welcome

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
        iv_book.setColorFilter(accentColor)
        // 避免从桌面启动程序后，会重新实例化入口类的activity
        if (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0) {
            finish()
            return
        }
        root_view.postDelayed({
            startActivity<MainActivity>()
            if (getPrefBoolean(getString(R.string.pk_default_read))) {
                startActivity<ReadBookActivity>()
            }
            finish()
        }, 200)
    }

}

class Launcher1 : WelcomeActivity()
class Launcher2 : WelcomeActivity()
class Launcher3 : WelcomeActivity()