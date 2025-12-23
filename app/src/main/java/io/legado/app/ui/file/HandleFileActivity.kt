package io.legado.app.ui.file

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppLog
import io.legado.app.databinding.ActivityTranslucenceBinding
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.help.IntentData
import io.legado.app.lib.dialogs.SelectItem
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.permission.Permissions
import io.legado.app.lib.permission.PermissionsCompat
import io.legado.app.utils.SelectImageContract
import io.legado.app.utils.checkWrite
import io.legado.app.utils.externalFiles
import io.legado.app.utils.getJsonArray
import io.legado.app.utils.isContentScheme
import io.legado.app.utils.launch
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import splitties.init.appCtx
import java.io.File

class HandleFileActivity :
    VMBaseActivity<ActivityTranslucenceBinding, HandleFileViewModel>(),
    FilePickerDialog.CallBack {

    override val binding by viewBinding(ActivityTranslucenceBinding::inflate)
    override val viewModel by viewModels<HandleFileViewModel>()
    private var mode = 0

    private val selectDocTree =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let {
                if (uri.isContentScheme()) {
                    val modeFlags =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(uri, modeFlags)
                }
                onResult(Intent().setData(uri))
            } ?: finish()
        }

    private val selectDoc = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let {
            if (it.isContentScheme()) {
                val modeFlags =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(it, modeFlags)
            }
            onResult(Intent().setData(it))
        } ?: finish()
    }

    private val selectImage = registerForActivityResult(SelectImageContract()) {
        it.uri?.let { uri ->
            onResult(Intent().setData(uri))
        } ?: finish()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        mode = intent.getIntExtra("mode", 0)
        viewModel.errorLiveData.observe(this) {
            toastOnUi(it)
            finish()
        }
        val allowExtensions = intent.getStringArrayExtra("allowExtensions")
        val selectList: ArrayList<SelectItem<Int>> = when (mode) {
            HandleFileContract.DIR_SYS -> getDirActions(true)
            HandleFileContract.DIR -> getDirActions()
            HandleFileContract.FILE -> getFileActions()
            HandleFileContract.EXPORT -> arrayListOf(
                SelectItem(getString(R.string.upload_url), 111)
            ).apply {
                addAll(getDirActions())
            }

            HandleFileContract.IMAGE -> getImageActions()
            else -> arrayListOf()
        }
        intent.getJsonArray<SelectItem<Int>>("otherActions")?.let {
            selectList.addAll(it)
        }
        val title = intent.getStringExtra("title") ?: let {
            when (mode) {
                HandleFileContract.EXPORT -> return@let getString(R.string.export)
                HandleFileContract.DIR -> return@let getString(R.string.select_folder)
                HandleFileContract.IMAGE -> return@let getString(R.string.select_image)
                else -> return@let getString(R.string.select_file)
            }
        }
        alert(title) {
            items(selectList) { _, item, _ ->
                when (item.value) {
                    HandleFileContract.DIR -> kotlin.runCatching {
                        selectDocTree.launch()
                    }.onFailure {
                        AppLog.put(getString(R.string.open_sys_dir_picker_error), it, true)
                        checkPermissions {
                            FilePickerDialog.show(
                                supportFragmentManager,
                                mode = HandleFileContract.DIR
                            )
                        }
                    }

                    HandleFileContract.FILE -> kotlin.runCatching {
                        selectDoc.launch(typesOfExtensions(allowExtensions))
                    }.onFailure {
                        AppLog.put(getString(R.string.open_sys_dir_picker_error), it, true)
                        checkPermissions {
                            FilePickerDialog.show(
                                supportFragmentManager,
                                mode = HandleFileContract.FILE,
                                allowExtensions = allowExtensions
                            )
                        }
                    }

                    HandleFileContract.IMAGE -> {
                        selectImage.launch()
                    }

                    10 -> checkPermissions {
                        @Suppress("DEPRECATION")
                        lifecycleScope.launchWhenResumed {
                            FilePickerDialog.show(
                                supportFragmentManager,
                                mode = HandleFileContract.DIR
                            )
                        }
                    }

                    11 -> checkPermissions {
                        @Suppress("DEPRECATION")
                        lifecycleScope.launchWhenResumed {
                            FilePickerDialog.show(
                                supportFragmentManager,
                                mode = HandleFileContract.FILE,
                                allowExtensions = allowExtensions
                            )
                        }
                    }

                    111 -> getFileData()?.let {
                        viewModel.upload(it.first, it.second, it.third) { url ->
                            val uri = url.toUri()
                            setResult(RESULT_OK, Intent().setData(uri))
                            finish()
                        }
                    }

                    112 -> checkPermissions { // 手动输入目录路径
                        showInputDirectoryDialog()
                    }

                    else -> {
                        val path = item.title
                        val uri = if (path.isContentScheme()) {
                            path.toUri()
                        } else {
                            Uri.fromFile(File(path))
                        }
                        onResult(Intent().setData(uri))
                    }
                }
            }
            onCancelled {
                finish()
            }
        }
    }

    private fun showInputDirectoryDialog() {
        val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
            editView.hint = getString(R.string.enter_directory_path)
        }

        alert(getString(R.string.manual_input)) {
            customView { alertBinding.root }
            okButton {
                val inputPath = alertBinding.editView.text.toString()
                if (inputPath.isBlank()) {
                    toastOnUi(getString(R.string.empty_directory_input))
                    return@okButton
                }
                val file = File(inputPath)
                if (file.exists() &&
                    file.isDirectory &&
                    isExternalStorage(file) &&
                    file.checkWrite()
                ) {
                    onResult(Intent().setData(Uri.fromFile(file)))
                } else {
                    toastOnUi(getString(R.string.invalid_directory))
                }
            }
            onDismiss {
                finish()
            }
            cancelButton()
        }
    }

    private fun isExternalStorage(path: File): Boolean {
        if (path.canonicalPath.startsWith(appCtx.externalFiles.parent!!)) {
            return false
        }
        try {
            if (Environment.isExternalStorageEmulated(path)) {
                return true
            }
        } catch (_: IllegalArgumentException) {
        }
        try {
            if (Environment.isExternalStorageRemovable(path)) {
                return true
            }
        } catch (_: IllegalArgumentException) {
        }
        return false
    }

    private fun getFileData(): Triple<String, Any, String>? {
        val fileName = intent.getStringExtra("fileName")
        val file = intent.getStringExtra("fileKey")?.let {
            IntentData.get<Any>(it)
        }
        val contentType = intent.getStringExtra("contentType")
        if (fileName != null && file != null && contentType != null) {
            return Triple(fileName, file, contentType)
        }
        return null
    }

    private fun getDirActions(onlySys: Boolean = false): ArrayList<SelectItem<Int>> {
        return if (onlySys) {
            arrayListOf(
                SelectItem(getString(R.string.sys_folder_picker), HandleFileContract.DIR),
                SelectItem(getString(R.string.manual_input), 112) // 添加手动输入选项
            )
        } else {
            arrayListOf(
                SelectItem(getString(R.string.sys_folder_picker), HandleFileContract.DIR),
                SelectItem(getString(R.string.app_folder_picker), 10),
                SelectItem(getString(R.string.manual_input), 112) // 添加手动输入选项
            )
        }
    }

    private fun getFileActions(): ArrayList<SelectItem<Int>> {
        return arrayListOf(
            SelectItem(getString(R.string.sys_file_picker), HandleFileContract.FILE),
            SelectItem(getString(R.string.app_file_picker), 11)
        )
    }

    private fun getImageActions(): ArrayList<SelectItem<Int>> {
        return arrayListOf(
            SelectItem(getString(R.string.sys_image_picker), HandleFileContract.IMAGE)
        ).apply {
            addAll(getFileActions())
        }
    }

    private fun checkPermissions(success: (() -> Unit)? = null) {
        PermissionsCompat.Builder()
            .addPermissions(*Permissions.Group.STORAGE)
            .rationale(R.string.tip_perm_request_storage)
            .onGranted {
                success?.invoke()
            }
            .onDenied {
                finish()
            }
            .onError {
                finish()
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
        val uri = data.data
        uri ?: let {
            finish()
            return
        }
        if (mode == HandleFileContract.EXPORT) {
            getFileData()?.let { fileData ->
                viewModel.saveToLocal(uri, fileData.first, fileData.second) { savedUri ->
                    setResult(RESULT_OK, Intent().setData(savedUri))
                    finish()
                }
            }
        } else {
            data.putExtra("value", intent.getStringExtra("value"))
            setResult(RESULT_OK, data)
            finish()
        }
    }
}