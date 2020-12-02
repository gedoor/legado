package io.legado.app.ui.config

import android.os.Bundle
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.EventBus
import io.legado.app.databinding.ActivityConfigBinding
import io.legado.app.utils.getViewModel
import io.legado.app.utils.observeEvent

class ConfigActivity : VMBaseActivity<ActivityConfigBinding, ConfigViewModel>() {
    override val viewModel: ConfigViewModel
        get() = getViewModel(ConfigViewModel::class.java)

    override fun getViewBinding(): ActivityConfigBinding {
        return ActivityConfigBinding.inflate(layoutInflater)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        intent.getIntExtra("configType", -1).let {
            if (it != -1) viewModel.configType = it
        }

        when (viewModel.configType) {
            ConfigViewModel.TYPE_CONFIG -> {
                binding.titleBar.title = getString(R.string.other_setting)
                val fTag = "otherConfigFragment"
                var configFragment = supportFragmentManager.findFragmentByTag(fTag)
                if (configFragment == null) configFragment = OtherConfigFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.configFrameLayout, configFragment, fTag)
                    .commit()
            }
            ConfigViewModel.TYPE_THEME_CONFIG -> {
                binding.titleBar.title = getString(R.string.theme_setting)
                val fTag = "themeConfigFragment"
                var configFragment = supportFragmentManager.findFragmentByTag(fTag)
                if (configFragment == null) configFragment = ThemeConfigFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.configFrameLayout, configFragment, fTag)
                    .commit()
            }
            ConfigViewModel.TYPE_WEB_DAV_CONFIG -> {
                binding.titleBar.title = getString(R.string.backup_restore)
                val fTag = "backupConfigFragment"
                var configFragment = supportFragmentManager.findFragmentByTag(fTag)
                if (configFragment == null) configFragment = BackupConfigFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.configFrameLayout, configFragment, fTag)
                    .commit()
            }
        }

    }

    override fun observeLiveBus() {
        super.observeLiveBus()
        observeEvent<String>(EventBus.RECREATE) {
            recreate()
        }
    }
}