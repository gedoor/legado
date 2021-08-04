package io.legado.app.ui.main.my

import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.preference.Preference
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.base.BasePreferenceFragment
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.databinding.FragmentMyConfigBinding
import io.legado.app.help.AppConfig
import io.legado.app.help.ThemeConfig
import io.legado.app.lib.theme.ATH
import io.legado.app.service.WebService
import io.legado.app.ui.about.AboutActivity
import io.legado.app.ui.about.DonateActivity
import io.legado.app.ui.about.ReadRecordActivity
import io.legado.app.ui.book.source.manage.BookSourceActivity
import io.legado.app.ui.config.ConfigActivity
import io.legado.app.ui.config.ConfigViewModel
import io.legado.app.ui.replace.ReplaceRuleActivity
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.ui.widget.prefs.NameListPreference
import io.legado.app.ui.widget.prefs.PreferenceCategory
import io.legado.app.ui.widget.prefs.SwitchPreference
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding

class MyFragment : BaseFragment(R.layout.fragment_my_config) {

    private val binding by viewBinding(FragmentMyConfigBinding::bind)

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(binding.titleBar.toolbar)
        val fragmentTag = "prefFragment"
        var preferenceFragment = childFragmentManager.findFragmentByTag(fragmentTag)
        if (preferenceFragment == null) preferenceFragment = PreferenceFragment()
        childFragmentManager.beginTransaction()
            .replace(R.id.pre_fragment, preferenceFragment, fragmentTag).commit()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.main_my, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_help -> {
                val text = String(requireContext().assets.open("help/appHelp.md").readBytes())
                TextDialog.show(childFragmentManager, text, TextDialog.MD)
            }
        }
    }

    /**
     * 配置
     */
    class PreferenceFragment : BasePreferenceFragment(),
        SharedPreferences.OnSharedPreferenceChangeListener {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            if (WebService.isRun) {
                putPrefBoolean(PreferKey.webService, true)
            } else {
                putPrefBoolean(PreferKey.webService, false)
            }
            addPreferencesFromResource(R.xml.pref_main)
            val webServicePre = findPreference<SwitchPreference>(PreferKey.webService)
            observeEventSticky<String>(EventBus.WEB_SERVICE) {
                webServicePre?.let {
                    it.isChecked = WebService.isRun
                    it.summary = if (WebService.isRun) {
                        WebService.hostAddress
                    } else {
                        getString(R.string.web_service_desc)
                    }
                }
            }
            findPreference<NameListPreference>(PreferKey.themeMode)?.let {
                it.setOnPreferenceChangeListener { _, _ ->
                    view?.post { ThemeConfig.applyDayNight(requireContext()) }
                    true
                }
            }
            if (AppConfig.isGooglePlay) {
                findPreference<PreferenceCategory>("aboutCategory")
                    ?.removePreferenceRecursively("donate")
            }
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            ATH.applyEdgeEffectColor(listView)
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
            super.onPause()
        }

        override fun onSharedPreferenceChanged(
            sharedPreferences: SharedPreferences?,
            key: String?
        ) {
            when (key) {
                PreferKey.webService -> {
                    if (requireContext().getPrefBoolean("webService")) {
                        WebService.start(requireContext())
                    } else {
                        WebService.stop(requireContext())
                    }
                }
                "recordLog" -> LogUtils.upLevel()
            }
        }

        override fun onPreferenceTreeClick(preference: Preference?): Boolean {
            when (preference?.key) {
                "bookSourceManage" -> startActivity<BookSourceActivity>()
                "replaceManage" -> startActivity<ReplaceRuleActivity>()
                "setting" -> startActivity<ConfigActivity> {
                    putExtra("configType", ConfigViewModel.TYPE_CONFIG)
                }
                "web_dav_setting" -> startActivity<ConfigActivity> {
                    putExtra("configType", ConfigViewModel.TYPE_WEB_DAV_CONFIG)
                }
                "theme_setting" -> startActivity<ConfigActivity> {
                    putExtra("configType", ConfigViewModel.TYPE_THEME_CONFIG)
                }
                "readRecord" -> startActivity<ReadRecordActivity>()
                "donate" -> startActivity<DonateActivity>()
                "about" -> startActivity<AboutActivity>()
            }
            return super.onPreferenceTreeClick(preference)
        }

    }
}