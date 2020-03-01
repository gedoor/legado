package io.legado.app.ui.welcome

import android.content.Intent
import android.os.Bundle
import com.github.houbb.opencc4j.util.ZhConverterUtil
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.help.AppConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.lib.theme.accentColor
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.main.MainActivity
import io.legado.app.utils.getPrefBoolean
import kotlinx.android.synthetic.main.activity_welcome.*
import org.jetbrains.anko.startActivity
import java.util.concurrent.TimeUnit

open class WelcomeActivity : BaseActivity(R.layout.activity_welcome) {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        iv_book.setColorFilter(accentColor)
        // 避免从桌面启动程序后，会重新实例化入口类的activity
        if (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0) {
            finish()
        } else {
            init()
        }
    }

    private fun init() {
        Coroutine.async {
            //清楚过期数据
            App.db.searchBookDao()
                .clearExpired(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1))
            //初始化简繁转换引擎
            when (AppConfig.chineseConverterType) {
                1 -> ZhConverterUtil.toSimple("初始化")
                2 -> ZhConverterUtil.toTraditional("初始化")
                else -> null
            }
        }
        root_view.postDelayed({ startMainActivity() }, 300)
    }

    private fun startMainActivity() {
        startActivity<MainActivity>()
        if (getPrefBoolean(getString(R.string.pk_default_read))) {
            startActivity<ReadBookActivity>()
        }
        finish()
    }

}

class Launcher1 : WelcomeActivity()
class Launcher2 : WelcomeActivity()
class Launcher3 : WelcomeActivity()