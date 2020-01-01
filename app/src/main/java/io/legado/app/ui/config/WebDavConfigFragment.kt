package io.legado.app.ui.config

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.documentfile.provider.DocumentFile
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.legado.app.R
import io.legado.app.help.IntentHelp
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
import io.legado.app.utils.LogUtils
import io.legado.app.utils.applyTint
import io.legado.app.utils.getPrefString

class WebDavConfigFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {

    private val oldDataRequestCode = 23156

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        fun bindPreferenceSummaryToValue(preference: Preference?) {
            preference?.apply {
                onPreferenceChangeListener = this@WebDavConfigFragment
                onPreferenceChange(
                    this,
                    context.getPrefString(key)
                )
            }
        }
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

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
            "web_dav_backup" -> PermissionsCompat.Builder(this)
                .addPermissions(*Permissions.Group.STORAGE)
                .rationale(R.string.tip_perm_request_storage)
                .onGranted { Backup.backup() }
                .request()
            "web_dav_restore" -> PermissionsCompat.Builder(this)
                .addPermissions(*Permissions.Group.STORAGE)
                .rationale(R.string.tip_perm_request_storage)
                .onGranted {
                    WebDavHelp.showRestoreDialog(requireContext())
                }
                .request()
            "import_old" -> importOldData()
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun importOldData() {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivityForResult(intent, oldDataRequestCode)
        } catch (e: Exception) {
            needInstallApps {
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

    private fun needInstallApps(callback: () -> Unit) {

        fun canRequestPackageInstalls() :Boolean {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                return requireContext().packageManager.canRequestPackageInstalls()
            }
            return true
        }
        if (!canRequestPackageInstalls()) {
            alert(title = "开启权限提示") {
                message = "需要打开「安装外部来源应用」权限才能导入旧版数据，请去设置中开启"
                yesButton {
                    IntentHelp.toInstallUnknown(requireContext())
                }
                noButton {
                }
            }.show().applyTint()
        } else {
            LogUtils.d("xxx","import old")
            callback()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            oldDataRequestCode -> if (resultCode == RESULT_OK) {
                data?.data?.let {
                    DocumentFile.fromTreeUri(requireContext(), it)?.listFiles()?.forEach {

                    }
                }
            }
        }
    }
}