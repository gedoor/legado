package io.legado.app.ui.config

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import io.legado.app.App
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.help.AppConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.permission.Permissions
import io.legado.app.help.permission.PermissionsCompat
import io.legado.app.help.storage.Backup
import io.legado.app.help.storage.Restore
import io.legado.app.help.storage.WebDavHelp
import io.legado.app.lib.dialogs.alert
import io.legado.app.utils.getPrefString
import io.legado.app.utils.isContentPath
import io.legado.app.utils.toast
import kotlinx.coroutines.Dispatchers.Main
import org.jetbrains.anko.toast

object BackupRestoreUi {

    private const val backupSelectRequestCode = 22
    private const val restoreSelectRequestCode = 33

    fun backup(fragment: Fragment) {
        val backupPath = AppConfig.backupPath
        if (backupPath.isNullOrEmpty()) {
            selectBackupFolder(fragment)
        } else {
            if (backupPath.isContentPath()) {
                val uri = Uri.parse(backupPath)
                val doc = DocumentFile.fromTreeUri(fragment.requireContext(), uri)
                if (doc?.canWrite() == true) {
                    Coroutine.async {
                        Backup.backup(fragment.requireContext(), backupPath)
                    }.onSuccess {
                        fragment.toast(R.string.backup_success)
                    }
                } else {
                    selectBackupFolder(fragment)
                }
            } else {
                backupUsePermission(fragment)
            }
        }
    }

    private fun backupUsePermission(fragment: Fragment, path: String = Backup.legadoPath) {
        PermissionsCompat.Builder(fragment)
            .addPermissions(*Permissions.Group.STORAGE)
            .rationale(R.string.tip_perm_request_storage)
            .onGranted {
                Coroutine.async {
                    Backup.backup(fragment.requireContext(), path)
                }.onSuccess {
                    fragment.toast(R.string.backup_success)
                }
            }
            .request()
    }

    fun selectBackupFolder(fragment: Fragment) {
        fragment.alert {
            titleResource = R.string.select_folder
            items(fragment.resources.getStringArray(R.array.select_folder).toList()) { _, index ->
                when (index) {
                    0 -> PermissionsCompat.Builder(fragment)
                        .addPermissions(*Permissions.Group.STORAGE)
                        .rationale(R.string.tip_perm_request_storage)
                        .onGranted {
                            AppConfig.backupPath = Backup.legadoPath
                            backupUsePermission(fragment)
                        }
                        .request()
                    1 -> {
                        try {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            fragment.startActivityForResult(intent, backupSelectRequestCode)
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                            fragment.toast(e.localizedMessage ?: "ERROR")
                        }
                    }
                    2 -> {

                    }
                }
            }
        }.show()
    }

    fun restore(fragment: Fragment) {
        Coroutine.async(context = Main) {
            if (!WebDavHelp.showRestoreDialog(fragment.requireContext()) {
                    fragment.toast(R.string.restore_success)
                }) {
                val backupPath = fragment.getPrefString(PreferKey.backupPath)
                if (backupPath?.isNotEmpty() == true) {
                    val uri = Uri.parse(backupPath)
                    val doc = DocumentFile.fromTreeUri(fragment.requireContext(), uri)
                    if (doc?.canWrite() == true) {
                        Restore.restore(fragment.requireContext(), uri)
                        fragment.toast(R.string.restore_success)
                    } else {
                        selectRestoreFolder(fragment)
                    }
                } else {
                    selectRestoreFolder(fragment)
                }
            }
        }
    }

    private fun selectRestoreFolder(fragment: Fragment) {
        fragment.alert {
            titleResource = R.string.select_folder
            items(fragment.resources.getStringArray(R.array.select_folder).toList()) { _, index ->
                when (index) {
                    0 -> PermissionsCompat.Builder(fragment)
                        .addPermissions(*Permissions.Group.STORAGE)
                        .rationale(R.string.tip_perm_request_storage)
                        .onGranted {
                            Coroutine.async {
                                AppConfig.backupPath = Backup.legadoPath
                                Restore.restore(Backup.legadoPath)
                            }.onSuccess {
                                fragment.toast(R.string.restore_success)
                            }
                        }
                        .request()
                    1 -> {
                        try {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            fragment.startActivityForResult(intent, restoreSelectRequestCode)
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                            fragment.toast(e.localizedMessage ?: "ERROR")
                        }
                    }
                    2 -> {

                    }
                }
            }
        }.show()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            backupSelectRequestCode -> if (resultCode == RESULT_OK) {
                data?.data?.let { uri ->
                    App.INSTANCE.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    AppConfig.backupPath = uri.toString()
                    Coroutine.async {
                        Backup.backup(App.INSTANCE, uri.toString())
                    }.onSuccess {
                        App.INSTANCE.toast(R.string.backup_success)
                    }
                }
            }
            restoreSelectRequestCode -> if (resultCode == RESULT_OK) {
                data?.data?.let { uri ->
                    App.INSTANCE.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    AppConfig.backupPath = uri.toString()
                    Coroutine.async {
                        Restore.restore(App.INSTANCE, uri)
                    }.onSuccess {
                        App.INSTANCE.toast(R.string.restore_success)
                    }
                }
            }
        }
    }

}