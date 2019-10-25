package io.legado.app.ui.filechooser

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.lib.theme.ATH
import io.legado.app.ui.filechooser.adapter.FileAdapter
import io.legado.app.ui.filechooser.adapter.PathAdapter
import io.legado.app.utils.FileUtils
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.dialog_file_chooser.*


class FileChooserDialog : DialogFragment(),
    FileAdapter.CallBack,
    PathAdapter.CallBack {

    companion object {
        const val tag = "FileChooserDialog"
        const val DIRECTORY = 0
        const val FILE = 1

        fun show(
            manager: FragmentManager,
            mode: Int = FILE,
            title: String? = null,
            isShowHomeDir: Boolean = false,
            isShowUpDir: Boolean = true,
            isShowHideDir: Boolean = false
        ) {
            val fragment = (manager.findFragmentByTag(tag) as? FileChooserDialog)
                ?: FileChooserDialog().apply {
                    this.mode = mode
                    this.title = title
                    this.isShowHomeDir = isShowHomeDir
                    this.isShowUpDir = isShowUpDir
                    this.isShowHideDir = isShowHideDir
                }
            fragment.show(manager, tag)
        }
    }

    override var allowExtensions: Array<String?>? = null
    override val isOnlyListDir: Boolean
        get() = mode == DIRECTORY
    override var isShowHomeDir: Boolean = false
    override var isShowUpDir: Boolean = true
    override var isShowHideDir: Boolean = false

    var title: String? = null
    private var initPath = FileUtils.getSdCardPath()
    private var mode: Int = FILE
    private lateinit var fileAdapter: FileAdapter
    private lateinit var pathAdapter: PathAdapter

    override fun onStart() {
        super.onStart()
        val dm = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(dm)
        dialog?.window?.setLayout((dm.widthPixels * 0.9).toInt(), (dm.heightPixels * 0.8).toInt())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_file_chooser, container, true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ATH.applyBackgroundTint(view)
        ATH.applyBackgroundTint(rv_path)
        tool_bar.title = title ?: let {
            if (isOnlyListDir) {
                getString(R.string.folder_chooser)
            } else {
                getString(R.string.file_chooser)
            }
        }

        fileAdapter = FileAdapter(requireContext(), this)
        pathAdapter = PathAdapter(requireContext(), this)

        rv_file.addItemDecoration(DividerItemDecoration(activity, LinearLayout.VERTICAL))
        rv_file.layoutManager = LinearLayoutManager(activity)
        rv_file.adapter = fileAdapter

        rv_path.layoutManager = LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
        rv_path.adapter = pathAdapter

        refreshCurrentDirPath(initPath)
    }

    override fun onFileClick(position: Int) {
        val fileItem = fileAdapter.getItem(position)
        if (fileItem?.isDirectory == true) {
            refreshCurrentDirPath(fileItem.path)
        } else {
            fileItem?.path?.let { path ->
                if (mode != DIRECTORY) {
                    (parentFragment as? CallBack)?.onFilePicked(path)
                    (activity as? CallBack)?.onFilePicked(path)
                    dismiss()
                }
            }
        }
    }

    override fun onPathClick(position: Int) {
        refreshCurrentDirPath(pathAdapter.getPath(position))
    }

    private fun refreshCurrentDirPath(currentPath: String) {
        if (currentPath == "/") {
            pathAdapter.updatePath("/")
        } else {
            pathAdapter.updatePath(currentPath)
        }
        fileAdapter.loadData(currentPath)
        var adapterCount = fileAdapter.itemCount
        if (isShowHomeDir) {
            adapterCount--
        }
        if (isShowUpDir) {
            adapterCount--
        }
        if (adapterCount < 1) {
            tv_empty.visible()
            tv_empty.setText(R.string.empty)
        } else {
            tv_empty.gone()
        }
    }

    interface CallBack {
        fun onFilePicked(currentPath: String)
    }
}