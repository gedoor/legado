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
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.help.permission.Permissions
import io.legado.app.help.permission.PermissionsCompat
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.dialog_font_select.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.CoroutineContext

class FontSelectDialog : DialogFragment(),
    Toolbar.OnMenuItemClickListener,
    CoroutineScope,
    FontAdapter.CallBack {
    private lateinit var job: Job
    private val fontFolderRequestCode = 35485
    private val fontFolder =
        App.INSTANCE.filesDir.absolutePath + File.separator + "Fonts" + File.separator
    private val fontCacheFolder =
        App.INSTANCE.cacheDir.absolutePath + File.separator + "Fonts" + File.separator
    override val coroutineContext: CoroutineContext
        get() = job + Main
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
        job = Job()
        return inflater.inflate(R.layout.dialog_font_select, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tool_bar.setTitle(R.string.select_font)
        tool_bar.inflateMenu(R.menu.font_select)
        tool_bar.setOnMenuItemClickListener(this)
        adapter = FontAdapter(requireContext(), this)
        recycler_view.layoutManager = LinearLayoutManager(context)
        recycler_view.adapter = adapter

        val fontPath = getPrefString(PreferKey.fontFolder)
        if (fontPath.isNullOrEmpty()) {
            openFolder()
        } else {
            val uri = Uri.parse(fontPath)
            if (DocumentFile.fromTreeUri(requireContext(), uri)?.canRead() == true) {
                getFontFiles(uri)
            } else {
                openFolder()
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_default -> {
                val cb = (parentFragment as? CallBack) ?: (activity as? CallBack)
                cb?.let {
                    if (it.curFontPath != "") {
                        it.selectFile("")
                    }
                }
                dismiss()
            }
            R.id.menu_other -> {
                openFolder()
            }
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun openFolder() {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivityForResult(intent, fontFolderRequestCode)
        } catch (e: java.lang.Exception) {
            PermissionsCompat.Builder(this)
                .addPermissions(*Permissions.Group.STORAGE)
                .rationale(R.string.tip_perm_request_storage)
                .onGranted { getFontFilesOld() }
                .request()
        }
    }

    @SuppressLint("DefaultLocale")
    private fun getFontFiles(uri: Uri) {
        launch(IO) {
            FileUtils.deleteFile(fontCacheFolder)
            DocumentFile.fromTreeUri(App.INSTANCE, uri)?.listFiles()?.forEach { file ->
                if (file.name?.toLowerCase()?.matches(".*\\.[ot]tf".toRegex()) == true) {
                    DocumentUtils.readBytes(App.INSTANCE, file.uri)?.let {
                        FileUtils.createFileIfNotExist(fontCacheFolder + file.name).writeBytes(it)
                    }
                }
            }
            try {
                val file = File(fontCacheFolder)
                file.listFiles { pathName ->
                    pathName.name.toLowerCase().matches(".*\\.[ot]tf".toRegex())
                }?.let {
                    withContext(Main) {
                        adapter?.setItems(it.toList())
                    }
                }
            } catch (e: Exception) {
                toast(e.localizedMessage ?: "")
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun getFontFilesOld() {
        try {
            val file =
                File(FileUtils.getSdCardPath() + File.separator + "Fonts")
            file.listFiles { pathName ->
                pathName.name.toLowerCase().matches(".*\\.[ot]tf".toRegex())
            }?.let {
                adapter?.setItems(it.toList())
            }
        } catch (e: Exception) {
            toast(e.localizedMessage ?: "")
        }
    }

    override fun onClick(file: File) {
        launch(IO) {
            file.copyTo(FileUtils.createFileIfNotExist(fontFolder + file.name), true)
                .absolutePath.let { path ->
                val cb = (parentFragment as? CallBack) ?: (activity as? CallBack)
                cb?.let {
                    if (it.curFontPath != path) {
                        withContext(Main) {
                            it.selectFile(path)
                        }
                    }
                }
            }
            dialog?.dismiss()
        }
    }

    override fun curFilePath(): String {
        return (parentFragment as? CallBack)?.curFontPath
            ?: (activity as? CallBack)?.curFontPath
            ?: ""
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            fontFolderRequestCode -> if (resultCode == RESULT_OK) {
                data?.data?.let { uri ->
                    putPrefString(PreferKey.fontFolder, uri.toString())
                    context?.contentResolver?.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    getFontFiles(uri)
                }
            }
        }
    }

    interface CallBack {
        fun selectFile(path: String)
        val curFontPath: String
    }
}