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
import io.legado.app.lib.dialogs.SelectItem
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.permission.Permissions
import io.legado.app.lib.permission.PermissionsCompat
import io.legado.app.utils.isContentScheme
import io.legado.app.utils.viewbindingdelegate.viewBinding
import java.io.File

class HandleFileActivity :
    BaseActivity<ActivityTranslucenceBinding>(
        theme = Theme.Transparent
    ), FilePickerDialog.CallBack {

    override val binding by viewBinding(ActivityTranslucenceBinding::inflate)

    private val selectDocTree =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
            it ?: let {
                finish()
                return@registerForActivityResult
            }
            if (it.isContentScheme()) {
                val modeFlags =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(it, modeFlags)
            }
            onResult(Intent().setData(it))
        }

    private val selectDoc = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        it ?: return@registerForActivityResult
        onResult(Intent().setData(it))
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        val mode = intent.getIntExtra("mode", 0)
        val allowExtensions = intent.getStringArrayExtra("allowExtensions")
        val selectList = when (mode) {
            HandleFileContract.DIR -> arrayListOf(
                SelectItem(getString(R.string.sys_folder_picker), HandleFileContract.DIR)
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    add(SelectItem(getString(R.string.app_folder_picker), 10))
                }
            }
            HandleFileContract.FILE -> arrayListOf(
                SelectItem(getString(R.string.sys_file_picker), HandleFileContract.FILE)
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    add(SelectItem(getString(R.string.app_folder_picker), 11))
                }
            }
            HandleFileContract.EXPORT -> arrayListOf(
                SelectItem(getString(R.string.upload_url), 111),
                SelectItem(getString(R.string.sys_folder_picker), HandleFileContract.DIR)
            )
            else -> arrayListOf()
        }
        intent.getParcelableArrayListExtra<SelectItem>("otherActions")?.let {
            selectList.addAll(it)
        }
        val title = intent.getStringExtra("title") ?: let {
            when (mode) {
                HandleFileContract.DIR -> {
                    return@let getString(R.string.select_folder)
                }
                else -> {
                    return@let getString(R.string.select_file)
                }
            }
        }
        alert(title) {
            items(selectList) { _, item, _ ->
                when (item.id) {
                    HandleFileContract.DIR -> selectDocTree.launch(null)
                    HandleFileContract.FILE -> selectDoc.launch(typesOfExtensions(allowExtensions))
                    10 -> checkPermissions {
                        FilePickerDialog.show(
                            supportFragmentManager,
                            mode = HandleFileContract.DIR
                        )
                    }
                    11 -> checkPermissions {
                        FilePickerDialog.show(
                            supportFragmentManager,
                            mode = HandleFileContract.FILE,
                            allowExtensions = allowExtensions
                        )
                    }
                    else -> {
                        val path = item.title
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