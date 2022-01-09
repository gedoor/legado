package io.legado.app.ui.association

import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.documentfile.provider.DocumentFile
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.databinding.ActivityTranslucenceBinding
import io.legado.app.help.AppConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.permission.Permissions
import io.legado.app.lib.permission.PermissionsCompat
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.document.HandleFileContract
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class FileAssociationActivity :
    VMBaseActivity<ActivityTranslucenceBinding, FileAssociationViewModel>() {

    private val localBookTreeSelect = registerForActivityResult(HandleFileContract()) {
        intent.data?.let { uri ->
            it.uri?.let { treeUri ->
                AppConfig.defaultBookTreeUri = treeUri.toString()
                importBook(treeUri, uri)
            } ?: let {
                toastOnUi("不选择文件夹重启应用后可能没有权限访问")
                viewModel.importBook(uri)
            }
        }
    }

    override val binding by viewBinding(ActivityTranslucenceBinding::inflate)

    override val viewModel by viewModels<FileAssociationViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.rotateLoading.show()
        viewModel.importBookLiveData.observe(this) { uri ->
            if (uri.isContentScheme()) {
                val treeUriStr = AppConfig.defaultBookTreeUri
                if (treeUriStr.isNullOrEmpty()) {
                    localBookTreeSelect.launch {
                        title = "选择保存书籍的文件夹"
                        mode = HandleFileContract.DIR_SYS
                    }
                } else {
                    importBook(Uri.parse(treeUriStr), uri)
                }
            } else {
                PermissionsCompat.Builder(this)
                    .addPermissions(*Permissions.Group.STORAGE)
                    .rationale(R.string.tip_perm_request_storage)
                    .onGranted {
                        viewModel.importBook(uri)
                    }.request()
            }
        }
        viewModel.onLineImportLive.observe(this) {
            startActivity<OnLineImportActivity> {
                data = it
            }
            finish()
        }
        viewModel.importBookSourceLive.observe(this) {
            binding.rotateLoading.hide()
            showDialogFragment(ImportBookSourceDialog(it, true))
        }
        viewModel.importRssSourceLive.observe(this) {
            binding.rotateLoading.hide()
            showDialogFragment(ImportRssSourceDialog(it, true))
        }
        viewModel.importReplaceRuleLive.observe(this) {
            binding.rotateLoading.hide()
            showDialogFragment(ImportReplaceRuleDialog(it, true))
        }
        viewModel.errorLiveData.observe(this) {
            binding.rotateLoading.hide()
            toastOnUi(it)
            finish()
        }
        viewModel.openBookLiveData.observe(this) {
            binding.rotateLoading.hide()
            startActivity<ReadBookActivity> {
                putExtra("bookUrl", it)
            }
            finish()
        }
        intent.data?.let { data ->
            viewModel.dispatchIndent(data, this::finallyDialog)
        }
    }

    private fun importBook(treeUri: Uri, uri: Uri) {
        launch {
            runCatching {
                if (treeUri.isContentScheme()) {
                    val treeDoc = DocumentFile.fromTreeUri(this@FileAssociationActivity, treeUri)
                    val bookDoc = DocumentFile.fromSingleUri(this@FileAssociationActivity, uri)
                    withContext(IO) {
                        val name = bookDoc?.name!!
                        var doc = treeDoc!!.findFile(name)
                        if (doc == null || bookDoc.lastModified() > doc.lastModified()) {
                            if (doc == null) {
                                doc = treeDoc.createFile(FileUtils.getMimeType(name), name)!!
                            }
                            contentResolver.openOutputStream(doc.uri)!!.use { oStream ->
                                contentResolver.openInputStream(bookDoc.uri)!!.use { iStream ->
                                    iStream.copyTo(oStream)
                                    oStream.flush()
                                }
                            }
                        }
                        viewModel.importBook(doc.uri)
                    }
                } else {
                    val treeFile = File(treeUri.path!!)
                    val bookDoc = DocumentFile.fromSingleUri(this@FileAssociationActivity, uri)
                    withContext(IO) {
                        val name = bookDoc?.name!!
                        val file = treeFile.getFile(name)
                        if (!file.exists() || file.lastModified() < bookDoc.lastModified()) {
                            FileOutputStream(file).use { oStream ->
                                contentResolver.openInputStream(bookDoc.uri)!!.use { iStream ->
                                    iStream.copyTo(oStream)
                                    oStream.flush()
                                }
                            }
                        }
                        viewModel.importBook(Uri.fromFile(file))
                    }
                }
            }.onFailure {
                toastOnUi(it.localizedMessage)
            }
        }
    }

    private fun finallyDialog(title: String, msg: String) {
        alert(title, msg) {
            okButton()
            onDismiss {
                finish()
            }
        }
    }

}
