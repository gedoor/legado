package io.legado.app.ui.widget.font

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.help.FileHelp
import io.legado.app.utils.DocumentUtils
import io.legado.app.utils.getPrefString
import kotlinx.android.synthetic.main.dialog_font_select.*
import java.io.File

class FontSelectDialog : DialogFragment(), FontAdapter.CallBack {
    private val fontFolderRequestCode = 35485
    private lateinit var adapter: FontAdapter
    var curPath: String? = null
    private val fontFolder =
        App.INSTANCE.filesDir.absolutePath + File.separator + "Fonts" + File.separator
    var defaultFont: (() -> Unit)? = null
    var selectFile: ((path: String) -> Unit)? = null

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tool_bar.setTitle(R.string.select_font)
        adapter = FontAdapter(requireContext(), this)
        recycler_view.layoutManager = LinearLayoutManager(context)
        recycler_view.adapter = adapter

        val fontPath = getPrefString(PreferKey.fontFolder)
        if (fontPath.isNullOrEmpty()) {
            try {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivityForResult(intent, fontFolderRequestCode)
            } catch (e: java.lang.Exception) {

            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun getFontFiles(uri: Uri) {
        DocumentFile.fromTreeUri(requireContext(), uri)?.listFiles()?.forEach { file ->
            if (file.name?.toLowerCase()?.matches(".*\\.[ot]tf".toRegex()) == true) {
                DocumentUtils.readBytes(App.INSTANCE, file.uri)?.let {
                    FileHelp.getFile(fontFolder + file.name).writeBytes(it)
                }
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun getFontFiles(): Array<File>? {
        return try {
            val file = File(fontFolder)
            file.listFiles { pathName ->
                pathName.name.toLowerCase().matches(".*\\.[ot]tf".toRegex())
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun onClick(file: File) {
        file.absolutePath.let {
            if (it != curPath) {
                selectFile?.invoke(it)
                dialog?.dismiss()
            }
        }
    }

    override fun curFilePath(): String {
        return curPath ?: ""
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            fontFolderRequestCode -> if (resultCode == RESULT_OK) {
                data?.data?.let { uri ->
                    getFontFiles(uri)
                }
            }
        }
    }

    interface CallBack {
        fun selectFile(uri: Uri)
    }
}