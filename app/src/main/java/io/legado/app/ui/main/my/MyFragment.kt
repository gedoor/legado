package io.legado.app.ui.main.my

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.documentfile.provider.DocumentFile
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.constant.Bus
import io.legado.app.constant.PreferKey
import io.legado.app.help.BookHelp
import io.legado.app.help.permission.Permissions
import io.legado.app.help.permission.PermissionsCompat
import io.legado.app.help.storage.Backup
import io.legado.app.help.storage.Restore
import io.legado.app.help.storage.WebDavHelp
import io.legado.app.lib.theme.ATH
import io.legado.app.service.WebService
import io.legado.app.ui.about.AboutActivity
import io.legado.app.ui.about.DonateActivity
import io.legado.app.ui.book.source.manage.BookSourceActivity
import io.legado.app.ui.config.ConfigActivity
import io.legado.app.ui.config.ConfigViewModel
import io.legado.app.ui.replacerule.ReplaceRuleActivity
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.view_title_bar.*
import kotlinx.coroutines.launch
import org.jetbrains.anko.startActivity

class MyFragment : BaseFragment(R.layout.fragment_my_config) {
    private val backupSelectRequestCode = 22
    private val restoreSelectRequestCode = 33

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
            R.id.menu_backup -> backup()
            R.id.menu_restore -> restore()
        }
    }


    private fun backup() {
        val backupPath = getPrefString(PreferKey.backupPath)
        if (backupPath?.isNotEmpty() == true) {
            val uri = Uri.parse(backupPath)
            val doc = DocumentFile.fromTreeUri(requireContext(), uri)
            if (doc?.canWrite() == true) {
                launch {
                    Backup.backup(requireContext(), uri)
                }
            } else {
                selectBackupFolder()
            }
        } else {
            selectBackupFolder()
        }
    }

    private fun selectBackupFolder() {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivityForResult(intent, backupSelectRequestCode)
        } catch (e: java.lang.Exception) {
            PermissionsCompat.Builder(this)
                .addPermissions(*Permissions.Group.STORAGE)
                .rationale(R.string.tip_perm_request_storage)
                .onGranted {
                    launch {
                        Backup.backup(requireContext(), null)
                    }
                }
                .request()
        }
    }

    fun restore() {
        launch {
            if (!WebDavHelp.showRestoreDialog(requireContext())) {
                val backupPath = getPrefString(PreferKey.backupPath)
                if (backupPath?.isNotEmpty() == true) {
                    val uri = Uri.parse(backupPath)
                    val doc = DocumentFile.fromTreeUri(requireContext(), uri)
                    if (doc?.canWrite() == true) {
                        Restore.restore(requireContext(), uri)
                        toast(R.string.restore_success)
                    } else {
                        selectBackupFolder()
                    }
                } else {
                    selectRestoreFolder()
                }
            }
        }
    }

    private fun selectRestoreFolder() {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivityForResult(intent, restoreSelectRequestCode)
        } catch (e: java.lang.Exception) {
            PermissionsCompat.Builder(this)
                .addPermissions(*Permissions.Group.STORAGE)
                .rationale(R.string.tip_perm_request_storage)
                .onGranted {
                    launch {
                        Restore.restore(Backup.legadoPath)
                        toast(R.string.restore_success)
                    }
                }
                .request()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            backupSelectRequestCode -> if (resultCode == Activity.RESULT_OK) {
                data?.data?.let { uri ->
                    requireContext().contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    putPrefString(PreferKey.backupPath, uri.toString())
                    launch {
                        Backup.backup(requireContext(), uri)
                    }
                }
            }
            restoreSelectRequestCode -> if (resultCode == Activity.RESULT_OK) {
                data?.data?.let { uri ->
                    requireContext().contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    putPrefString(PreferKey.backupPath, uri.toString())
                    launch {
                        Restore.restore(requireContext(), uri)
                        toast(R.string.restore_success)
                    }
                }
            }
        }
    }

    class PreferenceFragment : PreferenceFragmentCompat(),
        SharedPreferences.OnSharedPreferenceChangeListener {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            if (WebService.isRun) {
                putPrefBoolean("webService", true)
            } else {
                putPrefBoolean("webService", false)
            }
            addPreferencesFromResource(R.xml.pref_main)
            observeEvent<Boolean>(Bus.WEB_SERVICE_STOP) {
                findPreference<SwitchPreference>("webService")?.let {
                    it.isChecked = false
                }
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

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            when (key) {
                "isNightTheme" -> App.INSTANCE.applyDayNight()
                "webService" -> {
                    if (requireContext().getPrefBoolean("webService")) {
                        WebService.start(requireContext())
                        toast("正在启动服务\n具体信息查看通知栏")
                    }else{
                        WebService.stop(requireContext())
                        toast("服务已停止")
                    }
                }
                "recordLog" -> LogUtils.upLevel()
                "downloadPath" -> BookHelp.upDownloadPath()
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