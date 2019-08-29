package io.legado.app.ui.widget.font

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.utils.invisible
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.dialog_font_select.*
import java.io.File

class FontSelectDialog : DialogFragment(), FontAdapter.CallBack {

    private val defaultFolder =
        Environment.getExternalStorageDirectory().absolutePath + File.separator + "Fonts"
    lateinit var adapter: FontAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_font_select, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
    }

    private fun initData() {
        adapter = FontAdapter(requireContext(), this)
        recycler_view.layoutManager = LinearLayoutManager(requireContext())
        recycler_view.adapter = adapter
        val files = getFontFiles()
        if (files == null) {
            tv_no_data.visible()
        } else {
            tv_no_data.invisible()
            adapter.setItems(files.toList())
        }
    }

    @SuppressLint("DefaultLocale")
    private fun getFontFiles(fontFolder: String = defaultFolder): Array<File>? {
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

    }

    override fun curFilePath(): String {
        return ""
    }
}