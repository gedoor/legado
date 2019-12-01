package io.legado.app.ui.main.my

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
import io.legado.app.help.BookHelp
import io.legado.app.help.permission.Permissions
import io.legado.app.help.permission.PermissionsCompat
import io.legado.app.help.storage.Backup
import io.legado.app.help.storage.WebDavHelp
import io.legado.app.lib.theme.ATH
import io.legado.app.ui.about.AboutActivity
import io.legado.app.ui.about.DonateActivity
import io.legado.app.ui.book.source.manage.BookSourceActivity
import io.legado.app.ui.config.ConfigActivity
import io.legado.app.ui.config.ConfigViewModel
import io.legado.app.ui.replacerule.ReplaceRuleActivity
import io.legado.app.utils.LogUtils
import io.legado.app.utils.startActivity
import kotlinx.android.synthetic.main.view_title_bar.*
import org.jetbrains.anko.startActivity

class MyFragment : BaseFragment(R.layout.fragment_my_config) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(toolbar)
        val fragmentTag = "prefFragment"
        var preferenceFragment = childFragmentManager.findFragmentByTag(fragmentTag)
        if (preferenceFragment == null) preferenceFragment = PreferenceFragment()
        childFragmentManager.beginTransaction().replace(R.id.pre_fragment, preferenceFragment, fragmentTag).commit()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.main_my, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_help -> startActivity<AboutActivity>()
            R.id.menu_backup -> PermissionsCompat.Builder(this)
                .addPermissions(*Permissions.Group.STORAGE)
                .rationale(R.string.tip_perm_request_storage)
                .onGranted { Backup.backup() }
                .request()
            R.id.menu_restore -> PermissionsCompat.Builder(this)
                .addPermissions(*Permissions.Group.STORAGE)
                .rationale(R.string.tip_perm_request_storage)
                .onGranted { WebDavHelp.showRestoreDialog(requireContext()) }
                .request()
        }
    }

    class PreferenceFragment : PreferenceFragmentCompat(),
        SharedPreferences.OnSharedPreferenceChangeListener {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.pref_main)
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
                "isNightTheme" -> App.INSTANCE.applyDayNight()
                "recordLog" -> LogUtils.upLevel()
                "downloadPath" -> BookHelp.upDownloadPath()
                "showRss" -> activity?.recreate()
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