package io.legado.app.ui.widget.font

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.lib.dialogs.AlertBuilder
import io.legado.app.lib.dialogs.alert
import io.legado.app.utils.applyTint
import io.legado.app.utils.invisible
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.dialog_font_select.view.*
import java.io.File

class FontSelectDialog(context: Context) : FontAdapter.CallBack {

    private val defaultFolder =
        Environment.getExternalStorageDirectory().absolutePath + File.separator + "Fonts"
    private lateinit var adapter: FontAdapter
    private var builder: AlertBuilder<AlertDialog>
    private var dialog: AlertDialog? = null
    @SuppressLint("InflateParams")
    private var view: View = LayoutInflater.from(context).inflate(R.layout.dialog_font_select, null)
    var curPath: String? = null
    var fontFolder: String? = null
    var defaultFont: (() -> Unit)? = null
    var selectFile: ((path: String) -> Unit)? = null

    init {
        builder = context.alert(title = context.getString(R.string.select_font)) {
            customView = view
            positiveButton(R.string.default_font) { defaultFont?.invoke() }
            negativeButton(R.string.cancel)
        }
        initData()
    }

    fun show() {
        dialog = builder.show().applyTint()
    }

    private fun initData() = with(view) {
        adapter = FontAdapter(context, this@FontSelectDialog)
        recycler_view.layoutManager = LinearLayoutManager(context)
        recycler_view.adapter = adapter
        val files = getFontFiles()
        if (files.isNullOrEmpty()) {
            tv_no_data.visible()
        } else {
            tv_no_data.invisible()
            adapter.setItems(files.toList())
        }
    }

    @SuppressLint("DefaultLocale")
    private fun getFontFiles(): Array<File>? {
        val path = if (fontFolder.isNullOrEmpty()) {
            defaultFolder
        } else fontFolder
        return try {
            val file = File(path)
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
}