package io.legado.app.ui.main.my

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.help.AppConfig
import io.legado.app.help.ReadBookConfig
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.service.WebService
import io.legado.app.ui.about.AboutActivity
import io.legado.app.ui.about.DonateActivity
import io.legado.app.ui.book.source.manage.BookSourceActivity
import io.legado.app.ui.config.BackupRestoreUi
import io.legado.app.ui.config.ConfigActivity
import io.legado.app.ui.config.ConfigViewModel
import io.legado.app.ui.filechooser.FileChooserDialog
import io.legado.app.ui.replacerule.ReplaceRuleActivity
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.ui.widget.prefs.NameListPreference
import io.legado.app.ui.widget.prefs.PreferenceCategory
import io.legado.app.ui.widget.prefs.SwitchPreference
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.view_title_bar.*
import org.jetbrains.anko.startActivity

class MyFragment : BaseFragment(R.layout.fragment_my_config), FileChooserDialog.CallBack {

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(toolbar)
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
                val text = String(requireContext().assets.open("help.md").readBytes())
                TextDialog.show(childFragmentManager, text, TextDialog.MD)
            }
        }
    }

    override fun onFilePicked(requestCode: Int, currentPath: String) {
        BackupRestoreUi.onFilePicked(requestCode, currentPath)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        BackupRestoreUi.onActivityResult(requestCode, resultCode, data)
    }

    class PreferenceFragment : PreferenceFragmentCompat(),
        SharedPreferences.OnSharedPreferenceChangeListener {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            if (WebService.isRun) {
                putPrefBoolean(PreferKey.webService, true)
            } else {
                putPrefBoolean(PreferKey.webService, false)
            }
            addPreferencesFromResource(R.xml.pref_main)
            val webServicePre = findPreference<SwitchPreference>(PreferKey.webService)
            observeEvent<Boolean>(EventBus.WEB_SERVICE_STOP) {
                webServicePre?.isChecked = false
            }
            findPreference<NameListPreference>(PreferKey.themeMode)?.let {
                it.setOnPreferenceChangeListener { _, _ ->
                    view?.post { App.INSTANCE.applyDayNight() }
                    true
                }
            }
            if (AppConfig.isGooglePlay) {
                findPreference<PreferenceCategory>("aboutCategory")
                    ?.removePreference(findPreference("donate"))
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
                        toast(R.string.service_start)
                    } else {
                        WebService.stop(requireContext())
                        toast(R.string.service_stop)
                    }
                }
                "recordLog" -> LogUtils.upLevel()
                PreferKey.einkMode -> {
                    //既然是 E-Ink 模式，为什么不一步到位呢
                    if (AppConfig.isEInkMode) {
                        //保存开启前的设置
                        putPrefInt("lastReadBookPageAnim", ReadBookConfig.pageAnim)
                        putPrefInt("lastColorPrimary", ThemeStore.primaryColor(requireContext()))
                        putPrefInt("lastColorAccent", ThemeStore.accentColor(requireContext()))
                        putPrefInt("lastColorBackground", ThemeStore.backgroundColor(requireContext()))
                        putPrefInt("lastColorBottomBackground", ThemeStore.bottomBackground(requireContext()))
                        putPrefBoolean("lastIsNightTheme", AppConfig.isNightTheme)

                        //设置 E-Ink 模式配置
                        ReadBookConfig.pageAnim = 4
                        putPrefInt("colorPrimary", getCompatColor(R.color.white))
                        putPrefInt("colorAccent", getCompatColor(R.color.black))
                        putPrefInt("colorBackground", getCompatColor(R.color.white))
                        putPrefInt("colorBottomBackground", getCompatColor(R.color.white))
                        AppConfig.isNightTheme = false
                        App.INSTANCE.applyDayNight()
                        postEvent(EventBus.RECREATE, "")
                    } else {
                        ReadBookConfig.pageAnim = getPrefInt("lastReadBookPageAnim")
                        putPrefInt("colorPrimary", getPrefInt("lastColorPrimary"))
                        putPrefInt("colorAccent", getPrefInt("lastColorAccent"))
                        putPrefInt("colorBackground", getPrefInt("lastColorBackground"))
                        putPrefInt("colorBottomBackground", getPrefInt("lastColorBottomBackground"))
                        AppConfig.isNightTheme = getPrefBoolean("lastIsNightTheme")
                        App.INSTANCE.applyDayNight()
                        postEvent(EventBus.RECREATE, "")
                    }
                }
            }
        }

        override fun onPreferenceTreeClick(preference: Preference?): Boolean {
            when (preference?.key) {
                "bookSourceManage" -> context?.startActivity<BookSourceActivity>()
                "replaceManage" -> context?.startActivity<ReplaceRuleActivity>()
                "setting" -> context?.startActivity<ConfigActivity>(
                    Pair("configType", ConfigViewModel.TYPE_CONFIG)
                )
                "web_dav_setting" -> context?.startActivity<ConfigActivity>(
                    Pair("configType", ConfigViewModel.TYPE_WEB_DAV_CONFIG)
                )
                "theme_setting" -> context?.startActivity<ConfigActivity>(
                    Pair("configType", ConfigViewModel.TYPE_THEME_CONFIG)
                )
                "donate" -> context?.startActivity<DonateActivity>()
                "about" -> context?.startActivity<AboutActivity>()
            }
            return super.onPreferenceTreeClick(preference)
        }

    }
}