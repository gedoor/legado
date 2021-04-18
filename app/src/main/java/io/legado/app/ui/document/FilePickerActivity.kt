package io.legado.app.ui.document

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.MimeTypeMap
import androidx.activity.result.contract.ActivityResultContracts
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.constant.Theme
import io.legado.app.databinding.ActivityTranslucenceBinding
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.permission.Permissions
import io.legado.app.lib.permission.PermissionsCompat
import io.legado.app.utils.isContentScheme
import java.io.File

class FilePickerActivity :
    BaseActivity<ActivityTranslucenceBinding>(
        theme = Theme.Transparent
    ), FilePickerDialog.CallBack {

    private val selectDocTree =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
            if (it.isContentScheme()) {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            onResult(Intent().setData(it))
        }

    private val selectDoc = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        onResult(Intent().setData(it))
    }

    override fun getViewBinding(): ActivityTranslucenceBinding {
        return ActivityTranslucenceBinding.inflate(layoutInflater)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        val mode = intent.getIntExtra("mode", 0)
        val allowExtensions = intent.getStringArrayExtra("allowExtensions")
        val selectList = if (mode == FilePicker.DIRECTORY) {
            arrayListOf(getString(R.string.sys_folder_picker))
        } else {
            arrayListOf(getString(R.string.sys_file_picker))
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            selectList.add(getString(R.string.app_folder_picker))
        }
        intent.getStringArrayListExtra("otherActions")?.let {
            selectList.addAll(it)
        }
        val title = intent.getStringExtra("title") ?: let {
            if (mode == FilePicker.DIRECTORY) {
                return@let getString(R.string.select_folder)
            } else {
                return@let getString(R.string.select_file)
            }
        }
        alert(title) {
            items(selectList) { _, index ->
                when (index) {
                    0 -> if (mode == FilePicker.DIRECTORY) {
                        selectDocTree.launch(null)
                    } else {
                        selectDoc.launch(typesOfExtensions(allowExtensions))
                    }
                    1 -> if (mode == FilePicker.DIRECTORY) {
                        checkPermissions {
                            FilePickerDialog.show(
                                supportFragmentManager,
                                mode = FilePicker.DIRECTORY
                            )
                        }
                    } else {
                        checkPermissions {
                            FilePickerDialog.show(
                                supportFragmentManager,
                                mode = FilePicker.FILE,
                                allowExtensions = allowExtensions
                            )
                        }
                    }
                    else -> {
                        val path = selectList[index]
                        val uri = if (path.isContentScheme()) {
                            Uri.fromFile(File(path))
                        } else {
                            Uri.parse(path)
                        }
                        onResult(Intent().setData(uri))
                    }
                }
            }
            onCancelled {
                finish()
            }
        }.show()
    }

    private fun checkPermissions(success: (() -> Unit)? = null) {
        PermissionsCompat.Builder(this)
            .addPermissions(*Permissions.Group.STORAGE)
            .rationale(R.string.tip_perm_request_storage)
            .onGranted {
                success?.invoke()
            }
            .request()
    }

    private fun typesOfExtensions(allowExtensions: Array<String>?): Array<String> {
        val types = hashSetOf<String>()
        if (allowExtensions.isNullOrEmpty()) {
            types.add("*/*")
        } else {
            allowExtensions.forEach {
                when (it) {
                    "*" -> types.add("*/*")
                    "txt", "xml" -> types.add("text/*")
                    else -> {
                        val mime = MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(it)
                            ?: "application/octet-stream"
                        types.add(mime)
                    }
                }
            }
        }
        return types.toTypedArray()
    }

    override fun onResult(data: Intent) {
        if (data.data != null) {
            setResult(RESULT_OK, data)
        }
        finish()
    }

}