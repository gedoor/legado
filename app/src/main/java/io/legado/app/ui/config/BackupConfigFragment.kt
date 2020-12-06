package io.legado.app.ui.config

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import io.legado.app.R
import io.legado.app.base.BasePreferenceFragment
import io.legado.app.constant.PreferKey
import io.legado.app.help.LocalConfig
import io.legado.app.help.storage.Restore
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.accentColor
import io.legado.app.ui.filepicker.FilePickerDialog
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.applyTint
import io.legado.app.utils.getPrefString

class BackupConfigFragment : BasePreferenceFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener,
    FilePickerDialog.CallBack {

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
            ?.onLongClick = { BackupRestoreUi.restoreByFolder(this) }
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
            PreferKey.backupPath -> BackupRestoreUi.selectBackupFolder(this)
            PreferKey.restoreIgnore -> restoreIgnore()
            "web_dav_backup" -> BackupRestoreUi.backup(this)
            "web_dav_restore" -> BackupRestoreUi.restore(this)
            "import_old" -> BackupRestoreUi.importOldData(this)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        BackupRestoreUi.onActivityResult(requestCode, resultCode, data)
    }
}