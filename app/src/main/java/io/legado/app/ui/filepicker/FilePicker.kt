package io.legado.app.ui.filepicker

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.legado.app.R
import io.legado.app.help.permission.Permissions
import io.legado.app.help.permission.PermissionsCompat
import io.legado.app.lib.dialogs.alert

@Suppress("unused")
object FilePicker {

    fun selectFolder(
        activity: AppCompatActivity,
        requestCode: Int,
        title: String = activity.getString(R.string.select_folder),
        otherActions: List<String>? = null,
        otherFun: ((action: String) -> Unit)? = null
    ) {
        val selectList = arrayListOf(activity.getString(R.string.sys_folder_picker))
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            selectList.add(activity.getString(R.string.app_folder_picker))
        }
        otherActions?.let {
            selectList.addAll(otherActions)
        }
        activity.alert(title = title) {
            items(selectList) { _, index ->
                when (index) {
                    0 -> {
                        kotlin.runCatching {
                            val intent = createSelectDirIntent()
                            activity.startActivityForResult(intent, requestCode)
                        }.onFailure {
                            checkPermissions(activity) {
                                FilePickerDialog.show(
                                    activity.supportFragmentManager,
                                    requestCode,
                                    mode = FilePickerDialog.DIRECTORY
                                )
                            }
                        }
                    }
                    else -> {
                        val selectText = selectList[index]
                        if (selectText == activity.getString(R.string.app_folder_picker)) {
                            checkPermissions(activity) {
                                FilePickerDialog.show(
                                    activity.supportFragmentManager,
                                    requestCode,
                                    mode = FilePickerDialog.DIRECTORY
                                )
                            }
                        } else {
                            otherFun?.invoke(selectText)
                        }
                    }
                }
            }
        }.show()
    }

    fun selectFolder(
        fragment: Fragment,
        requestCode: Int,
        title: String = fragment.getString(R.string.select_folder),
        otherActions: List<String>? = null,
        otherFun: ((action: String) -> Unit)? = null
    ) {
        val selectList = arrayListOf(fragment.getString(R.string.sys_folder_picker))
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            selectList.add(fragment.getString(R.string.app_folder_picker))
        }
        otherActions?.let {
            selectList.addAll(otherActions)
        }
        fragment.alert(title = title) {
            items(selectList) { _, index ->
                when (index) {
                    0 -> {
                        kotlin.runCatching {
                            val intent = createSelectDirIntent()
                            fragment.startActivityForResult(intent, requestCode)
                        }.onFailure {
                            checkPermissions(fragment) {
                                FilePickerDialog.show(
                                    fragment.childFragmentManager,
                                    requestCode,
                                    mode = FilePickerDialog.DIRECTORY
                                )
                            }
                        }
                    }
                    else -> {
                        val selectText = selectList[index]
                        if (selectText == fragment.getString(R.string.app_folder_picker)) {
                            checkPermissions(fragment) {
                                FilePickerDialog.show(
                                    fragment.childFragmentManager,
                                    requestCode,
                                    mode = FilePickerDialog.DIRECTORY
                                )
                            }
                        } else {
                            otherFun?.invoke(selectText)
                        }
                    }
                }
            }
        }.show()
    }

    fun selectFile(
        activity: AppCompatActivity,
        requestCode: Int,
        title: String = activity.getString(R.string.select_file),
        allowExtensions: Array<String> = arrayOf(),
        otherActions: List<String>? = null,
        otherFun: ((action: String) -> Unit)? = null
    ) {
        val selectList = arrayListOf(activity.getString(R.string.sys_file_picker))
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            selectList.add(activity.getString(R.string.app_file_picker))
        }
        otherActions?.let {
            selectList.addAll(otherActions)
        }
        activity.alert(title = title) {
            items(selectList) { _, index ->
                when (index) {
                    0 -> {
                        kotlin.runCatching {
                            val intent = createSelectFileIntent()
                            intent.putExtra(
                                Intent.EXTRA_MIME_TYPES,
                                typesOfExtensions(allowExtensions)
                            )
                            activity.startActivityForResult(intent, requestCode)
                        }.onFailure {
                            checkPermissions(activity) {
                                FilePickerDialog.show(
                                    activity.supportFragmentManager,
                                    requestCode,
                                    mode = FilePickerDialog.FILE,
                                    allowExtensions = allowExtensions
                                )
                            }
                        }
                    }
                    else -> {
                        val selectText = selectList[index]
                        if (selectText == activity.getString(R.string.app_file_picker)) {
                            checkPermissions(activity) {
                                FilePickerDialog.show(
                                    activity.supportFragmentManager,
                                    requestCode,
                                    mode = FilePickerDialog.FILE,
                                    allowExtensions = allowExtensions
                                )
                            }
                        } else {
                            otherFun?.invoke(selectText)
                        }
                    }
                }
            }
        }.show()
    }

    fun selectFile(
        fragment: Fragment,
        requestCode: Int,
        title: String = fragment.getString(R.string.select_file),
        allowExtensions: Array<String> = arrayOf(),
        otherActions: List<String>? = null,
        otherFun: ((action: String) -> Unit)? = null
    ) {
        val selectList = arrayListOf(fragment.getString(R.string.sys_file_picker))
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            selectList.add(fragment.getString(R.string.app_file_picker))
        }
        otherActions?.let {
            selectList.addAll(otherActions)
        }
        fragment.alert(title = title) {
            items(selectList) { _, index ->
                when (index) {
                    0 -> {
                        kotlin.runCatching {
                            val intent = createSelectFileIntent()
                            intent.putExtra(
                                Intent.EXTRA_MIME_TYPES,
                                typesOfExtensions(allowExtensions)
                            )
                            fragment.startActivityForResult(intent, requestCode)
                        }.onFailure {
                            checkPermissions(fragment) {
                                FilePickerDialog.show(
                                    fragment.childFragmentManager,
                                    requestCode,
                                    mode = FilePickerDialog.FILE,
                                    allowExtensions = allowExtensions
                                )
                            }
                        }
                    }
                    else -> {
                        val selectText = selectList[index]
                        if (selectText == fragment.getString(R.string.app_file_picker)) {
                            checkPermissions(fragment) {
                                FilePickerDialog.show(
                                    fragment.childFragmentManager,
                                    requestCode,
                                    mode = FilePickerDialog.FILE,
                                    allowExtensions = allowExtensions
                                )
                            }
                        } else {
                            otherFun?.invoke(selectText)
                        }
                    }
                }
            }
        }.show()
    }

    private fun createSelectFileIntent(): Intent {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.type = "*/*"
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

    private fun typesOfExtensions(allowExtensions: Array<String>): Array<String> {
        val types = hashSetOf<String>()
        if (allowExtensions.isNullOrEmpty()) {
            types.add("*/*")
        } else {
            allowExtensions.forEach {
                when (it) {
                    "*" -> types.add("*/*")
                    "txt", "xml" -> types.add("text/*")
                    else -> types.add("application/$it")
                }
            }
        }
        return types.toTypedArray()
    }
}