package io.legado.app.ui.config

import android.os.Bundle
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_config.*
import kotlinx.android.synthetic.main.view_title_bar.*

class ConfigActivity : BaseActivity<ConfigViewModel>() {
    override val viewModel: ConfigViewModel
        get() = getViewModel(ConfigViewModel::class.java)
    override val layoutID: Int
        get() = R.layout.activity_config

    override fun onActivityCreated(viewModel: ConfigViewModel, savedInstanceState: Bundle?) {
        intent.getIntExtra("configType", -1).let {
            if (it != -1) viewModel.configType = it
        }
        this.setSupportActionBar(toolbar)

        when (viewModel.configType) {
            ConfigViewModel.TYPE_CONFIG -> {
                title_bar.title = "设置"
                val fTag = "configFragment"
                var configFragment = supportFragmentManager.findFragmentByTag(fTag)
                if (configFragment == null) configFragment = ConfigFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.configFrameLayout, configFragment, fTag)
                    .commit()
            }
            ConfigViewModel.TYPE_THEME_CONFIG -> {
                title_bar.title = "主题设置"
                val fTag = "themeConfigFragment"
                var configFragment = supportFragmentManager.findFragmentByTag(fTag)
                if (configFragment == null) configFragment = ThemeConfigFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.configFrameLayout, configFragment, fTag)
                    .commit()
            }
            ConfigViewModel.TYPE_WEB_DAV_CONFIG -> {
                title_bar.title = "WebDav设置"
                val fTag = "webDavFragment"
                var configFragment = supportFragmentManager.findFragmentByTag(fTag)
                if (configFragment == null) configFragment = WebDavConfigFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.configFrameLayout, configFragment, fTag)
                    .commit()
            }
        }

    }

}