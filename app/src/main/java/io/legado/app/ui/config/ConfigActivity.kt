package io.legado.app.ui.config

import android.os.Bundle
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_config.*
import kotlinx.android.synthetic.main.view_title_bar.*

class ConfigActivity : VMBaseActivity<ConfigViewModel>(R.layout.activity_config) {
    override val viewModel: ConfigViewModel
        get() = getViewModel(ConfigViewModel::class.java)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        intent.getIntExtra("configType", -1).let {
            if (it != -1) viewModel.configType = it
        }
        this.setSupportActionBar(toolbar)

        when (viewModel.configType) {
            ConfigViewModel.TYPE_CONFIG -> {
                title_bar.title = getString(R.string.other_setting)
                val fTag = "configFragment"
                var configFragment = supportFragmentManager.findFragmentByTag(fTag)
                if (configFragment == null) configFragment = ConfigFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.configFrameLayout, configFragment, fTag)
                    .commit()
            }
            ConfigViewModel.TYPE_THEME_CONFIG -> {
                title_bar.title = getString(R.string.theme_setting)
                val fTag = "themeConfigFragment"
                var configFragment = supportFragmentManager.findFragmentByTag(fTag)
                if (configFragment == null) configFragment = ThemeConfigFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.configFrameLayout, configFragment, fTag)
                    .commit()
            }
            ConfigViewModel.TYPE_WEB_DAV_CONFIG -> {
                title_bar.title = getString(R.string.backup_restore)
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