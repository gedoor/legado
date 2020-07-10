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

@Suppress("unused")
object FilePicker {

    fun selectFolder(
        activity: AppCompatActivity,
        requestCode: Int,
        title: String = activity.getString(R.string.select_folder),
        default: (() -> Unit)? = null
    ) {
        activity.alert(title = title) {
            val selectList =
                activity.resources.getStringArray(R.array.select_folder).toMutableList()
            default ?: let {
                selectList.removeAt(0)
            }
            items(selectList) { _, index ->
                when (if (default == null) index + 1 else index) {
                    0 -> default?.invoke()
                    1 -> {
                        try {
                            val intent = createSelectDirIntent()
                            activity.startActivityForResult(intent, requestCode)
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                            activity.toast(e.localizedMessage ?: "ERROR")
                        }
                    }
                    2 -> checkPermissions(activity) {
                        FileChooserDialog.show(
                            activity.supportFragmentManager,
                            requestCode,
                            mode = FileChooserDialog.DIRECTORY
                        )
                    }
                }
            }
        }.show()
    }

    fun selectFolder(
        fragment: Fragment,
        requestCode: Int,
        title: String = fragment.getString(R.string.select_folder),
        default: (() -> Unit)? = null
    ) {
        fragment.requireContext()
            .alert(title = title) {
                val selectList =
                    fragment.resources.getStringArray(R.array.select_folder).toMutableList()
                default ?: let {
                    selectList.removeAt(0)
                }
                items(selectList) { _, index ->
                    when (if (default == null) index + 1 else index) {
                        0 -> default?.invoke()
                        1 -> {
                            try {
                                val intent = createSelectDirIntent()
                                fragment.startActivityForResult(intent, requestCode)
                            } catch (e: java.lang.Exception) {
                                e.printStackTrace()
                                fragment.toast(e.localizedMessage ?: "ERROR")
                            }
                        }
                        2 -> checkPermissions(fragment) {
                            FileChooserDialog.show(
                                fragment.childFragmentManager,
                                requestCode,
                                mode = FileChooserDialog.DIRECTORY
                            )
                        }
                    }
                }
            }.show()
    }

    fun selectFile(
        activity: BaseActivity,
        requestCode: Int,
        title: String = activity.getString(R.string.select_file),
        type: Array<String>,
        allowExtensions: Array<String>?,
        default: (() -> Unit)? = null
    ) {
        activity.alert(title = title) {
            val selectList =
                activity.resources.getStringArray(R.array.select_folder).toMutableList()
            default ?: let {
                selectList.removeAt(0)
            }
            items(selectList) { _, index ->
                when (if (default == null) index + 1 else index) {
                    0 -> default?.invoke()
                    1 -> {
                        try {
                            val intent = createSelectFileIntent()
                            intent.putExtra(Intent.EXTRA_MIME_TYPES, type)
                            activity.startActivityForResult(intent, requestCode)
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                            activity.toast(e.localizedMessage ?: "ERROR")
                        }
                    }
                    2 -> checkPermissions(activity) {
                        FileChooserDialog.show(
                            activity.supportFragmentManager,
                            requestCode,
                            mode = FileChooserDialog.FILE,
                            allowExtensions = allowExtensions
                        )
                    }
                }
            }
        }.show()
    }

    fun selectFile(
        fragment: Fragment,
        requestCode: Int,
        title: String = fragment.getString(R.string.select_file),
        type: Array<String>,
        allowExtensions: Array<String>,
        default: (() -> Unit)? = null
    ) {
        fragment.requireContext()
            .alert(title = title) {
                val selectList =
                    fragment.resources.getStringArray(R.array.select_folder).toMutableList()
                default ?: let {
                    selectList.removeAt(0)
                }
                items(selectList) { _, index ->
                    when (if (default == null) index + 1 else index) {
                        0 -> default?.invoke()
                        1 -> {
                            try {
                                val intent = createSelectFileIntent()
                                intent.putExtra(Intent.EXTRA_MIME_TYPES, type)
                                fragment.startActivityForResult(intent, requestCode)
                            } catch (e: java.lang.Exception) {
                                e.printStackTrace()
                                fragment.toast(e.localizedMessage ?: "ERROR")
                            }
                        }
                        2 -> checkPermissions(fragment) {
                            FileChooserDialog.show(
                                fragment.childFragmentManager,
                                requestCode,
                                mode = FileChooserDialog.FILE,
                                allowExtensions = allowExtensions
                            )
                        }
                    }
                }
            }.show()
    }

    private fun createSelectFileIntent(): Intent {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return intent
    }

    private fun createSelectDirIntent(): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return intent
    }

    private fun checkPermissions(fragment: Fragment, success: (() -> Unit)? = null) {
        PermissionsCompat.Builder(fragment)
            .addPermissions(*Permissions.Group.STORAGE)
            .rationale(R.string.tip_perm_request_storage)
            .onGranted {
                success?.invoke()
            }
            .request()
    }

    private fun checkPermissions(activity: AppCompatActivity, success: (() -> Unit)? = null) {
        PermissionsCompat.Builder(activity)
            .addPermissions(*Permissions.Group.STORAGE)
            .rationale(R.string.tip_perm_request_storage)
            .onGranted {
                success?.invoke()
            }
            .request()
    }
}