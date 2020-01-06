package io.legado.app.ui.config

import android.app.Activity.RESULT_OK
import android.content.Intent
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
import io.legado.app.help.storage.Backup
import io.legado.app.help.storage.Restore
import io.legado.app.help.storage.WebDavHelp
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.noButton
import io.legado.app.lib.dialogs.yesButton
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import org.jetbrains.anko.toast
import kotlin.coroutines.CoroutineContext

class WebDavConfigFragment : PreferenceFragmentCompat(),
    Preference.OnPreferenceChangeListener,
    CoroutineScope {
    lateinit var job: Job
    private val oldDataRequestCode = 23156
    private val backupSelectRequestCode = 4567489
    private val restoreSelectRequestCode = 654872

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        job = Job()
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

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
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
            "web_dav_backup" -> backup()
            "web_dav_restore" -> restore()
            "import_old" -> importOldData()
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun backup() {
        val backupPath = getPrefString(PreferKey.backupPath)
        if (backupPath?.isEmpty() == true) {
            selectBackupFolder()
        } else {
            val uri = Uri.parse(backupPath)
            val doc = DocumentFile.fromTreeUri(requireContext(), uri)
            if (doc?.canWrite() == true) {
                Backup.backup(requireContext(), uri)
            } else {
                selectBackupFolder()
            }
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
                .onGranted { Backup.backup(requireContext(), null) }
                .request()
        }
    }

    fun restore() {
        launch {
            if (!WebDavHelp.showRestoreDialog(requireContext())) {
                val backupPath = getPrefString(PreferKey.backupPath)
                if (backupPath?.isEmpty() == true) {
                    selectRestoreFolder()
                } else {
                    val uri = Uri.parse(backupPath)
                    val doc = DocumentFile.fromTreeUri(requireContext(), uri)
                    if (doc?.canWrite() == true) {
                        Restore.restore(requireContext(), uri)
                    } else {
                        selectBackupFolder()
                    }
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
                .onGranted { Restore.restore(Backup.legadoPath) }
                .request()
        }
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
        when (requestCode) {
            oldDataRequestCode ->
                if (resultCode == RESULT_OK) data?.data?.let { uri ->
                    importOldData(uri)
                }
            backupSelectRequestCode -> if (resultCode == RESULT_OK) {
                data?.data?.let { uri ->
                    requireContext().contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    putPrefString(PreferKey.backupPath, uri.toString())
                    Backup.backup(requireContext(), uri)
                }
            }
            restoreSelectRequestCode -> if (resultCode == RESULT_OK) {
                data?.data?.let { uri ->
                    requireContext().contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    putPrefString(PreferKey.backupPath, uri.toString())
                    Restore.restore(requireContext(), uri)
                }
            }
        }
    }
}