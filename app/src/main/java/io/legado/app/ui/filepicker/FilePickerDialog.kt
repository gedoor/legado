package io.legado.app.ui.filepicker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.databinding.DialogFileChooserBinding
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.filepicker.adapter.FileAdapter
import io.legado.app.ui.filepicker.adapter.PathAdapter
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding
import java.io.File


class FilePickerDialog : DialogFragment(),
    Toolbar.OnMenuItemClickListener,
    FileAdapter.CallBack,
    PathAdapter.CallBack {

    companion object {
        const val tag = "FileChooserDialog"
        const val DIRECTORY = 0
        const val FILE = 1

        fun show(
            manager: FragmentManager,
            requestCode: Int,
            mode: Int = FILE,
            title: String? = null,
            initPath: String? = null,
            isShowHomeDir: Boolean = false,
            isShowUpDir: Boolean = true,
            isShowHideDir: Boolean = false,
            allowExtensions: Array<String>? = null,
            menus: Array<String>? = null
        ) {
            FilePickerDialog().apply {
                val bundle = Bundle()
                bundle.putInt("mode", mode)
                bundle.putInt("requestCode", requestCode)
                bundle.putString("title", title)
                bundle.putBoolean("isShowHomeDir", isShowHomeDir)
                bundle.putBoolean("isShowUpDir", isShowUpDir)
                bundle.putBoolean("isShowHideDir", isShowHideDir)
                bundle.putString("initPath", initPath)
                bundle.putStringArray("allowExtensions", allowExtensions)
                bundle.putStringArray("menus", menus)
                arguments = bundle
            }.show(manager, tag)
        }
    }

    private val binding by viewBinding(DialogFileChooserBinding::bind)
    override var allowExtensions: Array<String>? = null
    override val isSelectDir: Boolean
        get() = mode == DIRECTORY
    override var isShowHomeDir: Boolean = false
    override var isShowUpDir: Boolean = true
    override var isShowHideDir: Boolean = false

    private var requestCode: Int = 0
    var title: String? = null
    private var initPath = FileUtils.getSdCardPath()
    private var mode: Int = FILE
    private lateinit var fileAdapter: FileAdapter
    private lateinit var pathAdapter: PathAdapter
    private var menus: Array<String>? = null

    override fun onStart() {
        super.onStart()
        val dm = requireActivity().getSize()
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
        binding.toolBar.setBackgroundColor(primaryColor)
        view.setBackgroundResource(R.color.background_card)
        arguments?.let {
            requestCode = it.getInt("requestCode")
            mode = it.getInt("mode", FILE)
            title = it.getString("title")
            isShowHomeDir = it.getBoolean("isShowHomeDir")
            isShowUpDir = it.getBoolean("isShowUpDir")
            isShowHideDir = it.getBoolean("isShowHideDir")
            it.getString("initPath")?.let { path ->
                initPath = path
            }
            allowExtensions = it.getStringArray("allowExtensions")
            menus = it.getStringArray("menus")
        }
        binding.toolBar.title = title ?: let {
            if (isSelectDir) {
                getString(R.string.folder_chooser)
            } else {
                getString(R.string.file_chooser)
            }
        }
        initMenu()
        initContentView()
        refreshCurrentDirPath(initPath)
    }

    private fun initMenu() {
        binding.toolBar.inflateMenu(R.menu.file_chooser)
        if (isSelectDir) {
            binding.toolBar.menu.findItem(R.id.menu_ok).isVisible = true
        }
        menus?.let {
            it.forEach { menuTitle ->
                binding.toolBar.menu.add(menuTitle)
            }
        }
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
    }

    private fun initContentView() {
        fileAdapter = FileAdapter(requireContext(), this)
        pathAdapter = PathAdapter(requireContext(), this)

        binding.rvFile.addItemDecoration(VerticalDivider(requireContext()))
        binding.rvFile.layoutManager = LinearLayoutManager(activity)
        binding.rvFile.adapter = fileAdapter

        binding.rvPath.layoutManager = LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
        binding.rvPath.adapter = pathAdapter

    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_ok -> fileAdapter.currentPath?.let {
                setData(it)
                dismiss()
            }
            else -> item?.title?.let {
                (parentFragment as? CallBack)?.onMenuClick(it.toString())
                (activity as? CallBack)?.onMenuClick(it.toString())
                dismiss()
            }
        }
        return true
    }

    override fun onFileClick(position: Int) {
        val fileItem = fileAdapter.getItem(position)
        if (fileItem?.isDirectory == true) {
            refreshCurrentDirPath(fileItem.path)
        } else {
            fileItem?.path?.let { path ->
                if (mode == DIRECTORY) {
                    toast("这是文件夹选择,不能选择文件,点击右上角的确定选择文件夹")
                } else if (allowExtensions.isNullOrEmpty() ||
                    allowExtensions?.contains(FileUtils.getExtension(path)) == true
                ) {
                    setData(path)
                    dismiss()
                } else {
                    toast("不能打开此文件")
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
            binding.tvEmpty.visible()
            binding.tvEmpty.setText(R.string.empty)
        } else {
            binding.tvEmpty.gone()
        }
    }

    private fun setData(path: String) {
        val data = Intent().setData(Uri.fromFile(File(path)))
        (parentFragment as? CallBack)
            ?.onActivityResult(requestCode, Activity.RESULT_OK, data)
        (activity as? CallBack)
            ?.onActivityResult(requestCode, Activity.RESULT_OK, data)
    }

    interface CallBack {
        fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
        fun onMenuClick(menu: String) {}
    }
}