package io.legado.app.ui.filechooser

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.help.permission.Permissions
import io.legado.app.help.permission.PermissionsCompat
import io.legado.app.lib.dialogs.alert
import io.legado.app.utils.toast
import org.jetbrains.anko.toast

object FilePicker {

    fun selectFolder(activity: AppCompatActivity, requestCode: Int, default: (() -> Unit)? = null) {
        activity.alert(titleResource = R.string.select_folder) {
            val selectList =
                activity.resources.getStringArray(R.array.select_folder).toMutableList()
            default ?: let {
                selectList.removeAt(0)
            }
            items(selectList) { _, index ->
                when (index) {
                    0 -> default?.invoke()
                    1 -> {
                        try {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            activity.startActivityForResult(intent, requestCode)
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                            activity.toast(e.localizedMessage ?: "ERROR")
                        }
                    }
                    2 -> {
                        PermissionsCompat.Builder(activity)
                            .addPermissions(*Permissions.Group.STORAGE)
                            .rationale(R.string.tip_perm_request_storage)
                            .onGranted {
                                FileChooserDialog.show(
                                    activity.supportFragmentManager,
                                    requestCode,
                                    mode = FileChooserDialog.DIRECTORY
                                )
                            }
                            .request()
                    }
                }
            }
        }.show()
    }

    fun selectFolder(fragment: Fragment, requestCode: Int, default: (() -> Unit)? = null) {
        fragment.requireContext()
            .alert(titleResource = R.string.select_folder) {
                val selectList =
                    fragment.resources.getStringArray(R.array.select_folder).toMutableList()
                default ?: let {
                    selectList.removeAt(0)
                }
                items(selectList) { _, index ->
                    when (index) {
                        0 -> default?.invoke()
                        1 -> {
                            try {
                                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                fragment.startActivityForResult(intent, requestCode)
                            } catch (e: java.lang.Exception) {
                                e.printStackTrace()
                                fragment.toast(e.localizedMessage ?: "ERROR")
                            }
                        }
                        2 -> {
                            PermissionsCompat.Builder(fragment)
                                .addPermissions(*Permissions.Group.STORAGE)
                                .rationale(R.string.tip_perm_request_storage)
                                .onGranted {
                                    FileChooserDialog.show(
                                        fragment.childFragmentManager,
                                        requestCode,
                                        mode = FileChooserDialog.DIRECTORY
                                    )
                                }
                                .request()
                        }
                    }
                }
            }.show()
    }

    fun selectFile(
        activity: BaseActivity,
        requestCode: Int,
        type: String,
        allowExtensions: Array<String>?,
        default: (() -> Unit)? = null
    ) {
        activity.alert(titleResource = R.string.select_file) {
            val selectList =
                activity.resources.getStringArray(R.array.select_folder).toMutableList()
            default ?: let {
                selectList.removeAt(0)
            }
            items(selectList) { _, index ->
                when (index) {
                    0 -> default?.invoke()
                    1 -> {
                        try {
                            val intent = Intent(Intent.ACTION_GET_CONTENT)
                            intent.addCategory(Intent.CATEGORY_OPENABLE)
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            intent.type = type//设置类型
                            activity.startActivityForResult(intent, requestCode)
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                            activity.toast(e.localizedMessage ?: "ERROR")
                        }
                    }
                    2 -> {
                        PermissionsCompat.Builder(activity)
                            .addPermissions(*Permissions.Group.STORAGE)
                            .rationale(R.string.tip_perm_request_storage)
                            .onGranted {
                                FileChooserDialog.show(
                                    activity.supportFragmentManager,
                                    requestCode,
                                    mode = FileChooserDialog.FILE,
                                    allowExtensions = allowExtensions
                                )
                            }
                            .request()
                    }
                }
            }
        }.show()
    }

    fun selectFile(
        fragment: Fragment,
        requestCode: Int,
        type: String,
        allowExtensions: Array<String>,
        default: (() -> Unit)? = null
    ) {
        fragment.requireContext()
            .alert(titleResource = R.string.select_file) {
                val selectList =
                    fragment.resources.getStringArray(R.array.select_folder).toMutableList()
                default ?: let {
                    selectList.removeAt(0)
                }
                items(selectList) { _, index ->
                    when (index) {
                        0 -> default?.invoke()
                        1 -> {
                            try {
                                val intent = Intent(Intent.ACTION_GET_CONTENT)
                                intent.addCategory(Intent.CATEGORY_OPENABLE)
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                intent.type = type//设置类型
                                fragment.startActivityForResult(intent, requestCode)
                            } catch (e: java.lang.Exception) {
                                e.printStackTrace()
                                fragment.toast(e.localizedMessage ?: "ERROR")
                            }
                        }
                        2 -> {
                            PermissionsCompat.Builder(fragment)
                                .addPermissions(*Permissions.Group.STORAGE)
                                .rationale(R.string.tip_perm_request_storage)
                                .onGranted {
                                    FileChooserDialog.show(
                                        fragment.childFragmentManager,
                                        requestCode,
                                        mode = FileChooserDialog.FILE,
                                        allowExtensions = allowExtensions
                                    )
                                }
                                .request()
                        }
                    }
                }
            }.show()
    }

}