package io.legado.app.ui.config

import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.legado.app.App
import io.legado.app.R
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.permission.Permissions
import io.legado.app.help.permission.PermissionsCompat
import io.legado.app.help.storage.Backup
import io.legado.app.help.storage.Restore
import io.legado.app.help.storage.WebDavHelp
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.noButton
import io.legado.app.lib.dialogs.yesButton
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.applyTint
import io.legado.app.utils.getPrefString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WebDavConfigFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_config_web_dav)
        findPreference<EditTextPreference>("web_dav_url")?.let {
            it.setOnBindEditTextListener { editText ->
                ATH.setTint(editText, requireContext().accentColor)
            }
            bindPreferenceSummaryToValue(it)
        }
        findPreference<EditTextPreference>("web_dav_account")?.let {
            it.setOnBindEditTextListener { editText ->
                ATH.setTint(editText, requireContext().accentColor)
            }
            bindPreferenceSummaryToValue(it)
        }
        findPreference<EditTextPreference>("web_dav_password")?.let {
            it.setOnBindEditTextListener { editText ->
                ATH.setTint(editText, requireContext().accentColor)
                editText.inputType =
                    InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT
            }
            bindPreferenceSummaryToValue(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ATH.applyEdgeEffectColor(listView)
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        when {
            preference?.key == "web_dav_password" -> if (newValue == null) {
                preference.summary = getString(R.string.web_dav_pw_s)
            } else {
                preference.summary = "*".repeat(newValue.toString().length)
            }
            preference?.key == "web_dav_url" -> if (newValue == null) {
                preference.summary = getString(R.string.web_dav_url_s)
            } else {
                preference.summary = newValue.toString()
            }
            preference?.key == "web_dav_account" -> if (newValue == null) {
                preference.summary = getString(R.string.web_dav_account_s)
            } else {
                preference.summary = newValue.toString()
            }
            preference is ListPreference -> {
                val index = preference.findIndexOfValue(newValue?.toString())
                // Set the summary to reflect the new value.
                preference.setSummary(if (index >= 0) preference.entries[index] else null)
            }
            else -> preference?.summary = newValue?.toString()
        }
        return true
    }

    private fun bindPreferenceSummaryToValue(preference: Preference?) {
        preference?.apply {
            onPreferenceChangeListener = this@WebDavConfigFragment
            onPreferenceChange(
                this,
                context.getPrefString(key)
            )
        }
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
            "web_dav_backup" -> Backup.backup()
            "web_dav_restore" -> WebDavHelp.showRestoreDialog(requireContext())
            "import_old" -> importOld()
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun importOld() {
        Coroutine.async {
            if (withContext(Dispatchers.IO) { App.db.bookDao().allBookCount == 0 }) {
                alert(title = "导入") {
                    message = "是否导入旧版本数据"
                    yesButton {
                        PermissionsCompat.Builder(this@WebDavConfigFragment)
                            .addPermissions(*Permissions.Group.STORAGE)
                            .rationale(R.string.tip_perm_request_storage)
                            .onGranted {
                                Restore.importYueDuData(requireContext())
                            }
                            .request()
                    }
                    noButton {
                    }
                }.show().applyTint()
            }
        }
    }

}