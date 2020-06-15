package io.legado.app.ui.widget.font

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.help.AppConfig
import io.legado.app.help.permission.Permissions
import io.legado.app.help.permission.PermissionsCompat
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.filechooser.FileChooserDialog
import io.legado.app.ui.filechooser.FilePicker
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.dialog_font_select.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FontSelectDialog : BaseDialogFragment(),
    FileChooserDialog.CallBack,
    Toolbar.OnMenuItemClickListener,
    FontAdapter.CallBack {
    private val fontFolderRequestCode = 35485
    private val fontFolder by lazy {
        FileUtils.createFolderIfNotExist(App.INSTANCE.filesDir, "Fonts")
    }
    private val fontCacheFolder by lazy {
        FileUtils.createFolderIfNotExist(App.INSTANCE.cacheDir, "Fonts")
    }
    private var adapter: FontAdapter? = null

    override fun onStart() {
        super.onStart()
        val dm = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(dm)
        dialog?.window?.setLayout((dm.widthPixels * 0.9).toInt(), (dm.heightPixels * 0.9).toInt())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_font_select, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        tool_bar.setTitle(R.string.select_font)
        tool_bar.inflateMenu(R.menu.font_select)
        tool_bar.setOnMenuItemClickListener(this)
        recycler_view.isEnableScroll = !AppConfig.isEInkMode
        adapter = FontAdapter(requireContext(), this)
        recycler_view.layoutManager = LinearLayoutManager(context)
        recycler_view.adapter = adapter

        val fontPath = getPrefString(PreferKey.fontFolder)
        if (fontPath.isNullOrEmpty()) {
            openFolder()
        } else {
            if (fontPath.isContentPath()) {
                val doc = DocumentFile.fromTreeUri(requireContext(), Uri.parse(fontPath))
                if (doc?.canRead() == true) {
                    getFontFiles(doc)
                } else {
                    openFolder()
                }
            } else {
                getFontFilesByPermission(fontPath)
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_default -> {
                val requireContext = requireContext()
                requireContext.alert(titleResource = R.string.system_typeface) {
                    items(requireContext.resources.getStringArray(R.array.system_typefaces).toList()) { _, i ->
                        AppConfig.systemTypefaces = i
                        onDefaultFontChange()
                        dismiss()
                    }
                }.show()
            }
            R.id.menu_other -> {
                openFolder()
            }
        }
        return true
    }

    private fun openFolder() {
        launch(Main) {
            FilePicker.selectFolder(this@FontSelectDialog, fontFolderRequestCode) {
                val path = "${FileUtils.getSdCardPath()}${File.separator}Fonts"
                putPrefString(PreferKey.fontFolder, path)
                getFontFilesByPermission(path)
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun getFontFiles(doc: DocumentFile) {
        execute {
            val docItems = DocumentUtils.listFiles(App.INSTANCE, doc.uri)
            fontCacheFolder.listFiles()?.forEach { fontFile ->
                var contain = false
                for (item in docItems) {
                    if (fontFile.name == item.name) {
                        contain = true
                        break
                    }
                }
                if (!contain) {
                    fontFile.delete()
                }
            }
            docItems.forEach { item ->
                if (item.name.toLowerCase().matches(".*\\.[ot]tf".toRegex())) {
                    val fontFile = FileUtils.getFile(fontCacheFolder, item.name)
                    if (!fontFile.exists()) {
                        DocumentUtils.readBytes(App.INSTANCE, item.uri)?.let { byteArray ->
                            fontFile.writeBytes(byteArray)
                        }
                    }
                }
            }
            try {
                fontCacheFolder.listFiles { pathName ->
                    pathName.name.toLowerCase().matches(".*\\.[ot]tf".toRegex())
                }?.let {
                    withContext(Main) {
                        adapter?.setItems(it.toList())
                    }
                }
            } catch (e: Exception) {
                toast(e.localizedMessage ?: "")
            }
        }.onError {
            toast("getFontFiles:${it.localizedMessage}")
        }
    }

    @SuppressLint("DefaultLocale")
    private fun getFontFilesByPermission(path: String) {
        PermissionsCompat.Builder(this@FontSelectDialog)
            .addPermissions(*Permissions.Group.STORAGE)
            .rationale(R.string.tip_perm_request_storage)
            .onGranted {
                try {
                    val file = File(path)
                    file.listFiles { pathName ->
                        pathName.name.toLowerCase().matches(".*\\.[ot]tf".toRegex())
                    }?.let {
                        adapter?.setItems(it.toList())
                    }
                } catch (e: Exception) {
                    toast(e.localizedMessage ?: "")
                }
            }
            .request()
    }

    override fun onClick(file: File) {
        launch(IO) {
            file.copyTo(FileUtils.createFileIfNotExist(fontFolder, file.name), true)
                .absolutePath.let { path ->
                if (curFilePath != path) {
                    withContext(Main) {
                        callBack?.selectFile(path)
                    }
                }
            }
            dialog?.dismiss()
        }
    }

    override fun onFilePicked(requestCode: Int, currentPath: String) {
        when (requestCode) {
            fontFolderRequestCode -> {
                putPrefString(PreferKey.fontFolder, currentPath)
                getFontFilesByPermission(currentPath)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            fontFolderRequestCode -> if (resultCode == RESULT_OK) {
                data?.data?.let { uri ->
                    putPrefString(PreferKey.fontFolder, uri.toString())
                    val doc = DocumentFile.fromTreeUri(requireContext(), uri)
                    if (doc != null) {
                        context?.contentResolver?.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        getFontFiles(doc)
                    } else {
                        RealPathUtil.getPath(requireContext(), uri)?.let {
                            getFontFilesByPermission(it)
                        }
                    }
                }
            }
        }
    }

    private fun onDefaultFontChange() {
        if (curFilePath == "") {
            postEvent(EventBus.UP_CONFIG, true)
        } else {
            callBack?.selectFile("")
        }
    }

    override val curFilePath: String get() = callBack?.curFontPath ?: ""

    private val callBack: CallBack?
        get() = (parentFragment as? CallBack) ?: (activity as? CallBack)

    interface CallBack {
        fun selectFile(path: String)
        val curFontPath: String
    }
}