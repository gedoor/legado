package io.legado.app.ui.welcome

import android.content.Intent
import android.os.Bundle
import com.hankcs.hanlp.HanLP
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.constant.PreferKey
import io.legado.app.databinding.ActivityWelcomeBinding
import io.legado.app.help.AppConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.lib.theme.accentColor
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.main.MainActivity
import io.legado.app.utils.getPrefBoolean
import org.jetbrains.anko.startActivity
import java.util.concurrent.TimeUnit

open class WelcomeActivity : BaseActivity<ActivityWelcomeBinding>() {

    override fun getViewBinding(): ActivityWelcomeBinding {
        return ActivityWelcomeBinding.inflate(layoutInflater)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.ivBook.setColorFilter(accentColor)
        binding.vwTitleLine.setBackgroundColor(accentColor)
        // 避免从桌面启动程序后，会重新实例化入口类的activity
        if (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0) {
            finish()
        } else {
            init()
        }
    }

    private fun init() {
        Coroutine.async {
            App.db.cacheDao.clearDeadline(System.currentTimeMillis())
            //清除过期数据
            if (getPrefBoolean(PreferKey.autoClearExpired, true)) {
                App.db.searchBookDao
                    .clearExpired(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1))
            }
            //初始化简繁转换引擎
            when (AppConfig.chineseConverterType) {
                1 -> HanLP.convertToSimplifiedChinese("初始化")
                2 -> HanLP.convertToTraditionalChinese("初始化")
                else -> null
            }
        }
        binding.root.postDelayed({ startMainActivity() }, 500)
    }

    private fun startMainActivity() {
        startActivity<MainActivity>()
        if (getPrefBoolean(R.string.pk_default_read)) {
            startActivity<ReadBookActivity>()
        }
        finish()
    }

}

class Launcher1 : WelcomeActivity()
class Launcher2 : WelcomeActivity()
class Launcher3 : WelcomeActivity()
class Launcher4 : WelcomeActivity()
class Launcher5 : WelcomeActivity()
class Launcher6 : WelcomeActivity()