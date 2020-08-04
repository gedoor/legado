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
import io.legado.app.help.storage.ImportOldData
import io.legado.app.help.storage.Restore
import io.legado.app.help.storage.WebDavHelp
import io.legado.app.ui.filechooser.FilePicker
import io.legado.app.utils.getPrefString
import io.legado.app.utils.isContentPath
import io.legado.app.utils.toast
import kotlinx.coroutines.Dispatchers.Main
import org.jetbrains.anko.toast
import java.io.File

object BackupRestoreUi {
    private const val selectFolderRequestCode = 21
    private const val backupSelectRequestCode = 22
    private const val restoreSelectRequestCode = 33
    private const val oldDataRequestCode = 11

    fun backup(fragment: Fragment) {
        val backupPath = AppConfig.backupPath
        if (backupPath.isNullOrEmpty()) {
            selectBackupFolder(fragment, backupSelectRequestCode)
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
                    selectBackupFolder(fragment, backupSelectRequestCode)
                }
            } else {
                backupUsePermission(fragment, backupPath)
            }
        }
    }

    private fun backupUsePermission(
        fragment: Fragment,
        path: String
    ) {
        PermissionsCompat.Builder(fragment)
            .addPermissions(*Permissions.Group.STORAGE)
            .rationale(R.string.tip_perm_request_storage)
            .onGranted {
                Coroutine.async {
                    AppConfig.backupPath = path
                    Backup.backup(fragment.requireContext(), path)
                }.onSuccess {
                    fragment.toast(R.string.backup_success)
                }
            }
            .request()
    }

    fun selectBackupFolder(fragment: Fragment, requestCode: Int = selectFolderRequestCode) {
        FilePicker.selectFolder(fragment, requestCode)
    }

    fun restore(fragment: Fragment) {
        Coroutine.async(context = Main) {
            if (!WebDavHelp.showRestoreDialog(fragment.requireContext()) {
                    fragment.toast(R.string.restore_success)
                }) {
                val backupPath = fragment.getPrefString(PreferKey.backupPath)
                if (backupPath?.isNotEmpty() == true) {
                    if (backupPath.isContentPath()) {
                        val uri = Uri.parse(backupPath)
                        val doc = DocumentFile.fromTreeUri(fragment.requireContext(), uri)
                        if (doc?.canWrite() == true) {
                            Restore.restore(fragment.requireContext(), backupPath)
                        } else {
                            selectBackupFolder(fragment, restoreSelectRequestCode)
                        }
                    } else {
                        restoreUsePermission(fragment, backupPath)
                    }
                } else {
                    selectBackupFolder(fragment, restoreSelectRequestCode)
                }
            }
        }
    }

    private fun restoreUsePermission(fragment: Fragment, path: String) {
        PermissionsCompat.Builder(fragment)
            .addPermissions(*Permissions.Group.STORAGE)
            .rationale(R.string.tip_perm_request_storage)
            .onGranted {
                Coroutine.async {
                    AppConfig.backupPath = path
                    Restore.restoreDatabase(path)
                    Restore.restoreConfig(path)
                }.onSuccess {
                    fragment.toast(R.string.restore_success)
                }
            }
            .request()
    }

    fun importOldData(fragment: Fragment) {
        FilePicker.selectFolder(fragment, oldDataRequestCode)
    }

    fun onFilePicked(requestCode: Int, currentPath: String) {
        when (requestCode) {
            backupSelectRequestCode -> {
                AppConfig.backupPath = currentPath
                Coroutine.async {
                    Backup.backup(App.INSTANCE, currentPath)
                }.onSuccess {
                    App.INSTANCE.toast(R.string.backup_success)
                }
            }
            restoreSelectRequestCode -> {
                AppConfig.backupPath = currentPath
                Coroutine.async {
                    Restore.restore(App.INSTANCE, currentPath)
                }
            }
            selectFolderRequestCode -> {
                AppConfig.backupPath = currentPath
            }
            oldDataRequestCode -> {
                ImportOldData.import(App.INSTANCE, File(currentPath))
            }
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            backupSelectRequestCode -> if (resultCode == RESULT_OK) {
                data?.data?.let { uri ->
                    App.INSTANCE.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
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
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    AppConfig.backupPath = uri.toString()
                    Coroutine.async {
                        Restore.restore(App.INSTANCE, uri.toString())
                    }
                }
            }
            selectFolderRequestCode -> if (resultCode == RESULT_OK) {
                data?.data?.let { uri ->
                    App.INSTANCE.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    AppConfig.backupPath = uri.toString()
                }
            }
            oldDataRequestCode -> if (resultCode == RESULT_OK) {
                data?.data?.let { uri ->
                    ImportOldData.importUri(uri)
                }
            }
        }
    }

}