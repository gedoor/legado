package io.legado.app.ui.config

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import io.legado.app.R
import io.legado.app.constant.Permissions
import io.legado.app.constant.PreferKey
import io.legado.app.help.AppConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.storage.Backup
import io.legado.app.help.storage.BookWebDav
import io.legado.app.help.storage.ImportOldData
import io.legado.app.help.storage.Restore
import io.legado.app.ui.filepicker.FilePicker
import io.legado.app.utils.getPrefString
import io.legado.app.utils.isContentScheme
import io.legado.app.utils.longToast
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.Dispatchers.Main
import splitties.init.appCtx

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
            if (backupPath.isContentScheme()) {
                val uri = Uri.parse(backupPath)
                val doc = DocumentFile.fromTreeUri(fragment.requireContext(), uri)
                if (doc?.canWrite() == true) {
                    Coroutine.async {
                        Backup.backup(fragment.requireContext(), backupPath)
                    }.onSuccess {
                        fragment.toastOnUi(R.string.backup_success)
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
        fragment.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            var hasPermission = true
            it.forEach { (t, u) ->
                if (!u) {
                    hasPermission = false
                    fragment.toastOnUi(t)
                }
            }
            if (hasPermission) {
                Coroutine.async {
                    AppConfig.backupPath = path
                    Backup.backup(fragment.requireContext(), path)
                }.onSuccess {
                    fragment.toastOnUi(R.string.backup_success)
                }
            }
        }.launch(Permissions.Group.STORAGE)
    }

    fun selectBackupFolder(fragment: Fragment, requestCode: Int = selectFolderRequestCode) {
        FilePicker.selectFolder(fragment, requestCode)
    }

    fun restore(fragment: Fragment) {
        Coroutine.async(context = Main) {
            BookWebDav.showRestoreDialog(fragment.requireContext())
        }.onError {
            fragment.longToast("WebDavError:${it.localizedMessage}\n将从本地备份恢复。")
            val backupPath = fragment.getPrefString(PreferKey.backupPath)
            if (backupPath?.isNotEmpty() == true) {
                if (backupPath.isContentScheme()) {
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

    fun restoreByFolder(fragment: Fragment) {
        selectBackupFolder(fragment, restoreSelectRequestCode)
    }

    private fun restoreUsePermission(fragment: Fragment, path: String) {
        fragment.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            Coroutine.async {
                AppConfig.backupPath = path
                Restore.restoreDatabase(path)
                Restore.restoreConfig(path)
            }
        }.launch(Permissions.Group.STORAGE)
    }

    fun importOldData(fragment: Fragment) {
        FilePicker.selectFolder(fragment, oldDataRequestCode)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            backupSelectRequestCode -> if (resultCode == RESULT_OK) {
                data?.data?.let { uri ->
                    if (uri.isContentScheme()) {
                        appCtx.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
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
            }
            restoreSelectRequestCode -> if (resultCode == RESULT_OK) {
                data?.data?.let { uri ->
                    if (uri.isContentScheme()) {
                        appCtx.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
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
            }
            selectFolderRequestCode -> if (resultCode == RESULT_OK) {
                data?.data?.let { uri ->
                    if (uri.isContentScheme()) {
                        appCtx.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        AppConfig.backupPath = uri.toString()
                    } else {
                        AppConfig.backupPath = uri.path
                    }
                }
            }
            oldDataRequestCode -> if (resultCode == RESULT_OK) {
                data?.data?.let { uri ->
                    ImportOldData.importUri(appCtx, uri)
                }
            }
        }
    }

}