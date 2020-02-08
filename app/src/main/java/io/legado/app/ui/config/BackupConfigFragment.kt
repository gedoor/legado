package io.legado.app.ui.config

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
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
import io.legado.app.constant.PreferKey
import io.legado.app.help.IntentHelp
import io.legado.app.help.permission.Permissions
import io.legado.app.help.permission.PermissionsCompat
import io.legado.app.help.storage.Restore
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.noButton
import io.legado.app.lib.dialogs.yesButton
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.DocumentUtils
import io.legado.app.utils.LogUtils
import io.legado.app.utils.applyTint
import io.legado.app.utils.getPrefString
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import org.jetbrains.anko.toast
import kotlin.coroutines.CoroutineContext

class BackupConfigFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener,
    CoroutineScope {
    private lateinit var job: Job
    private val oldDataRequestCode = 11

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        job = Job()
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

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
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
            "web_dav_backup" -> BackupRestoreUi.backup(this)
            "web_dav_restore" -> BackupRestoreUi.restore(this)
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
                        PermissionsCompat.Builder(this@BackupConfigFragment)
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

    private fun importOldData(uri: Uri) {
        launch(IO) {
            DocumentFile.fromTreeUri(requireContext(), uri)?.listFiles()?.forEach {
                when (it.name) {
                    "myBookShelf.json" ->
                        try {
                            DocumentUtils.readText(requireContext(), it.uri)?.let { json ->
                                val importCount = Restore.importOldBookshelf(json)
                                withContext(Dispatchers.Main) {
                                    requireContext().toast("成功导入书籍${importCount}")
                                }
                            }
                        } catch (e: java.lang.Exception) {
                            withContext(Dispatchers.Main) {
                                requireContext().toast("导入书籍失败\n${e.localizedMessage}")
                            }
                        }
                    "myBookSource.json" ->
                        try {
                            DocumentUtils.readText(requireContext(), it.uri)?.let { json ->
                                val importCount = Restore.importOldSource(json)
                                withContext(Dispatchers.Main) {
                                    requireContext().toast("成功导入书源${importCount}")
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                requireContext().toast("导入源失败\n${e.localizedMessage}")
                            }
                        }
                    "myBookReplaceRule.json" ->
                        try {
                            DocumentUtils.readText(requireContext(), it.uri)?.let { json ->
                                val importCount = Restore.importOldReplaceRule(json)
                                withContext(Dispatchers.Main) {
                                    requireContext().toast("成功导入替换规则${importCount}")
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                requireContext().toast("导入替换规则失败\n${e.localizedMessage}")
                            }
                        }
                }
            }
        }
    }

    private fun needInstallApps(callback: () -> Unit) {

        fun canRequestPackageInstalls(): Boolean {
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
            LogUtils.d("xxx", "import old")
            callback()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        BackupRestoreUi.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            oldDataRequestCode ->
                if (resultCode == RESULT_OK) data?.data?.let { uri ->
                    importOldData(uri)
                }
        }
    }
}