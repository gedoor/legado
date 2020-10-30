package io.legado.app.ui.filepicker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.help.permission.Permissions
import io.legado.app.help.permission.PermissionsCompat
import io.legado.app.lib.dialogs.alert
import io.legado.app.utils.applyTint
import io.legado.app.utils.toast
import org.jetbrains.anko.toast

@Suppress("unused")
object FilePicker {

    fun selectFolder(
        activity: AppCompatActivity,
        requestCode: Int,
        title: String = activity.getString(R.string.select_folder),
        otherActions: List<String>? = null,
        otherFun: ((action: String) -> Unit)? = null
    ) {
        val selectList =
            activity.resources.getStringArray(R.array.select_folder).toMutableList()
        otherActions?.let {
            selectList.addAll(otherActions)
        }
        activity.alert(title = title) {
            items(selectList) { _, index ->
                when (index) {
                    0 -> {
                        try {
                            val intent = createSelectDirIntent()
                            activity.startActivityForResult(intent, requestCode)
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                            activity.toast(e.localizedMessage ?: "ERROR")
                        }
                    }
                    1 -> checkPermissions(activity) {
                        FilePickerDialog.show(
                            activity.supportFragmentManager,
                            requestCode,
                            mode = FilePickerDialog.DIRECTORY
                        )
                    }
                    else -> otherFun?.invoke(selectList[index])
                }
            }
        }.show().applyTint()
    }

    fun selectFolder(
        fragment: Fragment,
        requestCode: Int,
        title: String = fragment.getString(R.string.select_folder),
        otherActions: List<String>? = null,
        otherFun: ((action: String) -> Unit)? = null
    ) {
        val selectList =
            fragment.resources.getStringArray(R.array.select_folder).toMutableList()
        otherActions?.let {
            selectList.addAll(otherActions)
        }
        fragment.alert(title = title) {
            items(selectList) { _, index ->
                when (index) {
                    0 -> {
                        try {
                            val intent = createSelectDirIntent()
                            fragment.startActivityForResult(intent, requestCode)
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                            fragment.toast(e.localizedMessage ?: "ERROR")
                        }
                    }
                    1 -> checkPermissions(fragment) {
                        FilePickerDialog.show(
                            fragment.childFragmentManager,
                            requestCode,
                            mode = FilePickerDialog.DIRECTORY
                        )
                    }
                    else -> otherFun?.invoke(selectList[index])
                }
            }
        }.show().applyTint()
    }

    fun selectFile(
        activity: BaseActivity,
        requestCode: Int,
        title: String = activity.getString(R.string.select_file),
        allowExtensions: Array<String>,
        otherActions: List<String>? = null,
        otherFun: ((action: String) -> Unit)? = null
    ) {
        val selectList =
            activity.resources.getStringArray(R.array.select_folder).toMutableList()
        otherActions?.let {
            selectList.addAll(otherActions)
        }
        activity.alert(title = title) {
            items(selectList) { _, index ->
                when (index) {
                    0 -> {
                        try {
                            val intent = createSelectFileIntent()
                            intent.putExtra(
                                Intent.EXTRA_MIME_TYPES,
                                typesOfExtensions(allowExtensions)
                            )
                            activity.startActivityForResult(intent, requestCode)
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                            activity.toast(e.localizedMessage ?: "ERROR")
                        }
                    }
                    1 -> checkPermissions(activity) {
                        FilePickerDialog.show(
                            activity.supportFragmentManager,
                            requestCode,
                            mode = FilePickerDialog.FILE,
                            allowExtensions = allowExtensions
                        )
                    }
                    else -> otherFun?.invoke(selectList[index])
                }
            }
        }.show().applyTint()
    }

    fun selectFile(
        fragment: Fragment,
        requestCode: Int,
        title: String = fragment.getString(R.string.select_file),
        allowExtensions: Array<String>,
        otherActions: List<String>? = null,
        otherFun: ((action: String) -> Unit)? = null
    ) {
        val selectList =
            fragment.resources.getStringArray(R.array.select_folder).toMutableList()
        otherActions?.let {
            selectList.addAll(otherActions)
        }
        fragment.alert(title = title) {
            items(selectList) { _, index ->
                when (index) {
                    0 -> {
                        try {
                            val intent = createSelectFileIntent()
                            intent.putExtra(
                                Intent.EXTRA_MIME_TYPES,
                                typesOfExtensions(allowExtensions)
                            )
                            fragment.startActivityForResult(intent, requestCode)
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                            fragment.toast(e.localizedMessage ?: "ERROR")
                        }
                    }
                    1 -> checkPermissions(fragment) {
                        FilePickerDialog.show(
                            fragment.childFragmentManager,
                            requestCode,
                            mode = FilePickerDialog.FILE,
                            allowExtensions = allowExtensions
                        )
                    }
                    else -> otherFun?.invoke(selectList[index])
                }
            }
        }.show().applyTint()
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
        allowExtensions.forEach {
            when (it) {
                "txt", "xml" -> types.add("text/*")
                else -> types.add("application/$it")
            }
        }
        return types.toTypedArray()
    }
}