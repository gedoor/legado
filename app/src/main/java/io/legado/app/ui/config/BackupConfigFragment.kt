package io.legado.app.ui.config

import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.documentfile.provider.DocumentFile
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import io.legado.app.R
import io.legado.app.base.BasePreferenceFragment
import io.legado.app.constant.PreferKey
import io.legado.app.help.AppConfig
import io.legado.app.help.LocalConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.storage.Backup
import io.legado.app.help.storage.BookWebDav
import io.legado.app.help.storage.ImportOldData
import io.legado.app.help.storage.Restore
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.permission.Permissions
import io.legado.app.lib.permission.PermissionsCompat
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.accentColor
import io.legado.app.ui.document.FilePicker
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers
import splitties.init.appCtx

class BackupConfigFragment : BasePreferenceFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val selectBackupPath = registerForActivityResult(FilePicker()) { uri ->
        uri ?: return@registerForActivityResult
        if (uri.isContentScheme()) {
            AppConfig.backupPath = uri.toString()
        } else {
            AppConfig.backupPath = uri.path
        }
    }
    private val backupDir = registerForActivityResult(FilePicker()) { uri ->
        uri ?: return@registerForActivityResult
        if (uri.isContentScheme()) {
            AppConfig.backupPath = uri.toString()
            Coroutine.async {
                Backup.backup(appCtx, uri.toString())
            }.onSuccess {
                appCtx.toastOnUi(R.string.backup_success)
            }
        } else {
            uri.path?.let { path ->
                AppConfig.backupPath = path
                Coroutine.async {
                    Backup.backup(appCtx, path)
                }.onSuccess {
                    appCtx.toastOnUi(R.string.backup_success)
                }
            }
        }
    }
    private val restoreDir = registerForActivityResult(FilePicker()) { uri ->
        uri ?: return@registerForActivityResult
        if (uri.isContentScheme()) {
            AppConfig.backupPath = uri.toString()
            Coroutine.async {
                Restore.restore(appCtx, uri.toString())
            }
        } else {
            uri.path?.let { path ->
                AppConfig.backupPath = path
                Coroutine.async {
                    Restore.restore(appCtx, path)
                }
            }
        }
    }
    private val restoreOld = registerForActivityResult(FilePicker()) { uri ->
        uri?.let {
            ImportOldData.importUri(appCtx, uri)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_config_backup)
        findPreference<EditTextPreference>(PreferKey.webDavUrl)?.let {
            it.setOnBindEditTextListener { editText ->
                ATH.setTint(editText, requireContext().accentColor)
            }

        }
        findPreference<EditTextPreference>(PreferKey.webDavAccount)?.let {
            it.setOnBindEditTextListener { editText ->
                ATH.setTint(editText, requireContext().accentColor)
            }
        }
        findPreference<EditTextPreference>(PreferKey.webDavPassword)?.let {
            it.setOnBindEditTextListener { editText ->
                ATH.setTint(editText, requireContext().accentColor)
                editText.inputType =
                    InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT
            }
        }
        upPreferenceSummary(PreferKey.webDavUrl, getPrefString(PreferKey.webDavUrl))
        upPreferenceSummary(PreferKey.webDavAccount, getPrefString(PreferKey.webDavAccount))
        upPreferenceSummary(PreferKey.webDavPassword, getPrefString(PreferKey.webDavPassword))
        upPreferenceSummary(PreferKey.backupPath, getPrefString(PreferKey.backupPath))
        findPreference<io.legado.app.ui.widget.prefs.Preference>("web_dav_restore")
            ?.onLongClick = { restoreDir.launch(null) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        ATH.applyEdgeEffectColor(listView)
        setHasOptionsMenu(true)
        if (!LocalConfig.backupHelpVersionIsLast) {
            showHelp()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.backup_restore, menu)
        menu.applyTint(requireContext())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_help -> showHelp()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showHelp() {
        val text = String(requireContext().assets.open("help/webDavHelp.md").readBytes())
        TextDialog.show(childFragmentManager, text, TextDialog.MD)
    }

    override fun onDestroy() {
        super.onDestroy()
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PreferKey.webDavUrl,
            PreferKey.webDavAccount,
            PreferKey.webDavPassword,
            PreferKey.backupPath -> {
                upPreferenceSummary(key, getPrefString(key))
            }
        }
    }

    private fun upPreferenceSummary(preferenceKey: String, value: String?) {
        val preference = findPreference<Preference>(preferenceKey) ?: return
        when (preferenceKey) {
            PreferKey.webDavUrl ->
                if (value == null) {
                    preference.summary = getString(R.string.web_dav_url_s)
                } else {
                    preference.summary = value.toString()
                }
            PreferKey.webDavAccount ->
                if (value == null) {
                    preference.summary = getString(R.string.web_dav_account_s)
                } else {
                    preference.summary = value.toString()
                }
            PreferKey.webDavPassword ->
                if (value == null) {
                    preference.summary = getString(R.string.web_dav_pw_s)
                } else {
                    preference.summary = "*".repeat(value.toString().length)
                }
            else -> {
                if (preference is ListPreference) {
                    val index = preference.findIndexOfValue(value)
                    // Set the summary to reflect the new value.
                    preference.summary = if (index >= 0) preference.entries[index] else null
                } else {
                    preference.summary = value
                }
            }
        }
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
            PreferKey.backupPath -> selectBackupPath.launch(null)
            PreferKey.restoreIgnore -> restoreIgnore()
            "web_dav_backup" -> backup()
            "web_dav_restore" -> restore()
            "import_old" -> restoreOld.launch(null)
        }
        return super.onPreferenceTreeClick(preference)
    }


    private fun restoreIgnore() {
        val checkedItems = BooleanArray(Restore.ignoreKeys.size) {
            Restore.ignoreConfig[Restore.ignoreKeys[it]] ?: false
        }
        alert(R.string.restore_ignore) {
            multiChoiceItems(Restore.ignoreTitle, checkedItems) { _, which, isChecked ->
                Restore.ignoreConfig[Restore.ignoreKeys[which]] = isChecked
            }
            onDismiss {
                Restore.saveIgnoreConfig()
            }
        }.show()
    }


    fun backup() {
        val backupPath = AppConfig.backupPath
        if (backupPath.isNullOrEmpty()) {
            backupDir.launch(null)
        } else {
            if (backupPath.isContentScheme()) {
                val uri = Uri.parse(backupPath)
                val doc = DocumentFile.fromTreeUri(requireContext(), uri)
                if (doc?.canWrite() == true) {
                    Coroutine.async {
                        Backup.backup(requireContext(), backupPath)
                    }.onSuccess {
                        toastOnUi(R.string.backup_success)
                    }
                } else {
                    backupDir.launch(null)
                }
            } else {
                backupUsePermission(backupPath)
            }
        }
    }

    private fun backupUsePermission(path: String) {
        PermissionsCompat.Builder(this)
            .addPermissions(*Permissions.Group.STORAGE)
            .rationale(R.string.tip_perm_request_storage)
            .onGranted {
                Coroutine.async {
                    AppConfig.backupPath = path
                    Backup.backup(requireContext(), path)
                }.onSuccess {
                    toastOnUi(R.string.backup_success)
                }
            }
            .request()
    }

    fun restore() {
        Coroutine.async(context = Dispatchers.Main) {
            BookWebDav.showRestoreDialog(requireContext())
        }.onError {
            longToast("WebDavError:${it.localizedMessage}\n将从本地备份恢复。")
            val backupPath = getPrefString(PreferKey.backupPath)
            if (backupPath?.isNotEmpty() == true) {
                if (backupPath.isContentScheme()) {
                    val uri = Uri.parse(backupPath)
                    val doc = DocumentFile.fromTreeUri(requireContext(), uri)
                    if (doc?.canWrite() == true) {
                        Restore.restore(requireContext(), backupPath)
                    } else {
                        restoreDir.launch(null)
                    }
                } else {
                    restoreUsePermission(backupPath)
                }
            } else {
                restoreDir.launch(null)
            }
        }
    }

    private fun restoreUsePermission(path: String) {
        PermissionsCompat.Builder(this)
            .addPermissions(*Permissions.Group.STORAGE)
            .rationale(R.string.tip_perm_request_storage)
            .onGranted {
                Coroutine.async {
                    AppConfig.backupPath = path
                    Restore.restoreDatabase(path)
                    Restore.restoreConfig(path)
                }
            }
            .request()
    }

}