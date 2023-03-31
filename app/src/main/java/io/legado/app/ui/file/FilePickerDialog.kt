package io.legado.app.ui.file

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.databinding.DialogFileChooserBinding
import io.legado.app.databinding.ItemFilePickerBinding
import io.legado.app.databinding.ItemPathPickerBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.getPrimaryDisabledTextColor
import io.legado.app.lib.theme.getPrimaryTextColor
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.file.HandleFileContract.Companion.FILE
import io.legado.app.ui.file.utils.FilePickerIcon
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding
import java.io.File


class FilePickerDialog : BaseDialogFragment(R.layout.dialog_file_chooser),
    Toolbar.OnMenuItemClickListener {

    companion object {
        const val tag = "FileChooserDialog"

        fun show(
            manager: FragmentManager,
            mode: Int = FILE,
            title: String? = null,
            initPath: String? = null,
            isShowHideDir: Boolean = false,
            allowExtensions: Array<String>? = null,
        ) {
            FilePickerDialog().apply {
                val bundle = Bundle()
                bundle.putInt("mode", mode)
                bundle.putString("title", title)
                bundle.putBoolean("isShowHideDir", isShowHideDir)
                bundle.putString("initPath", initPath)
                bundle.putStringArray("allowExtensions", allowExtensions)
                arguments = bundle
            }.show(manager, tag)
        }
    }

    private val binding by viewBinding(DialogFileChooserBinding::bind)
    private val viewModel by viewModels<FilePickerViewModel>()
    private val dirParent = ".."
    private val pathAdapter by lazy { PathAdapter() }
    private val fileAdapter by lazy { FileAdapter() }

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, 0.8f)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        view.setBackgroundResource(R.color.background_card)
        binding.toolBar.title = arguments?.getString("title") ?: let {
            if (viewModel.isSelectDir) {
                getString(R.string.folder_chooser)
            } else {
                getString(R.string.file_chooser)
            }
        }
        initMenu()
        initContentView()
        viewModel.filesLiveData.observe(viewLifecycleOwner) {
            fileAdapter.selectFile = null
            fileAdapter.setItems(it)
        }
        viewModel.initData(arguments)
    }

    private fun initMenu() {
        binding.toolBar.inflateMenu(R.menu.file_chooser)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
    }

    private fun initContentView() {
        binding.rvPath.layoutManager = LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
        binding.rvPath.adapter = pathAdapter

        binding.rvFile.addItemDecoration(VerticalDivider(requireContext()))
        binding.rvFile.layoutManager = LinearLayoutManager(activity)
        binding.rvFile.adapter = fileAdapter

        binding.tvOk.setOnClickListener {
            if (viewModel.isSelectDir) {
                viewModel.lastDir?.let {
                    setResultData(it.path)
                    dismissAllowingStateLoss()
                }
            } else {
                val file = fileAdapter.selectFile
                if (file == null) {
                    toastOnUi("请选择文件")
                } else {
                    setResultData(file.path)
                    dismissAllowingStateLoss()
                }
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_create -> alert(R.string.create_folder) {
                val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                    editView.hint = "文件夹名"
                }
                customView { alertBinding.root }
                okButton {
                    val text = alertBinding.editView.text?.toString()
                    if (text.isNullOrBlank()) {
                        toastOnUi("文件夹名不能为空")
                    } else {
                        viewModel.createFolder(text.trim())
                    }
                }
                cancelButton()
            }
        }
        return true
    }

    private fun setResultData(path: String) {
        val data = Intent().setData(Uri.fromFile(File(path)))
        (parentFragment as? CallBack)?.onResult(data)
        (activity as? CallBack)?.onResult(data)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        activity?.finish()
    }

    @SuppressLint("SetTextI18n")
    inner class PathAdapter :
        RecyclerAdapter<File, ItemPathPickerBinding>(requireContext()) {

        private val arrowIcon = ConvertUtils.toDrawable(FilePickerIcon.getArrow())

        init {
            addHeaderView {
                ItemPathPickerBinding.inflate(inflater, it, false).apply {
                    textView.text = "root"
                    imageView.setImageDrawable(arrowIcon)
                    root.setOnClickListener {
                        viewModel.subDocs.clear()
                        setItems(viewModel.subDocs)
                        viewModel.upFiles(viewModel.rootDoc)
                    }
                }
            }
        }

        override fun getViewBinding(parent: ViewGroup): ItemPathPickerBinding {
            return ItemPathPickerBinding.inflate(inflater, parent, false).apply {
                imageView.setImageDrawable(arrowIcon)
            }
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemPathPickerBinding) {
            binding.root.setOnClickListener {
                viewModel.subDocs = viewModel.subDocs.subList(0, holder.layoutPosition)
                setItems(viewModel.subDocs)
                viewModel.upFiles(viewModel.subDocs.lastOrNull())
            }
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemPathPickerBinding,
            item: File,
            payloads: MutableList<Any>
        ) {
            binding.textView.text = item.name
        }

    }

    inner class FileAdapter : RecyclerAdapter<File, ItemFilePickerBinding>(requireContext()) {
        private val primaryTextColor = context.getPrimaryTextColor(!AppConfig.isNightTheme)
        private val disabledTextColor = context.getPrimaryDisabledTextColor(!AppConfig.isNightTheme)
        private val upIcon = ConvertUtils.toDrawable(FilePickerIcon.getUpDir())!!
        private val folderIcon = ConvertUtils.toDrawable(FilePickerIcon.getFolder())!!
        private val fileIcon = ConvertUtils.toDrawable(FilePickerIcon.getFile())!!
        private val selectDrawable =
            ResourcesCompat.getDrawable(resources, R.drawable.shape_radius_1dp, null)!!.apply {
                DrawableCompat.setTint(this, primaryTextColor)
            }
        var selectFile: File? = null

        override fun getViewBinding(parent: ViewGroup): ItemFilePickerBinding {
            return ItemFilePickerBinding.inflate(inflater, parent, false)
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemFilePickerBinding) {
            binding.root.setOnClickListener {
                val item = getItemByLayoutPosition(holder.layoutPosition)
                item?.let {
                    if (item == viewModel.lastDir) {
                        viewModel.subDocs.removeLastOrNull()
                        pathAdapter.setItems(viewModel.subDocs)
                        viewModel.upFiles(viewModel.subDocs.lastOrNull() ?: viewModel.rootDoc)
                    } else if (item.isDirectory) {
                        viewModel.subDocs.add(item)
                        pathAdapter.setItems(viewModel.subDocs)
                        viewModel.upFiles(item)
                    } else if (viewModel.isSelectFile) {
                        viewModel.allowExtensions.let {
                            if (it.isNullOrEmpty() || it.contains(FileUtils.getExtension(item.path))) {
                                selectFile = item
                                notifyItemRangeChanged(getHeaderCount(), itemCount, "selectFile")
                            }
                        }
                    }
                }
            }
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemFilePickerBinding,
            item: File,
            payloads: MutableList<Any>
        ) {
            if (payloads.isEmpty()) {
                if (item == viewModel.lastDir) {
                    binding.imageView.setImageDrawable(upIcon)
                    binding.textView.text = dirParent
                } else if (item.isDirectory) {
                    binding.imageView.setImageDrawable(folderIcon)
                    binding.textView.text = item.name
                } else {
                    binding.imageView.setImageDrawable(fileIcon)
                    binding.textView.text = item.name
                }
                if (item.isDirectory) {
                    binding.textView.setTextColor(primaryTextColor)
                } else {
                    if (viewModel.isSelectDir) {
                        binding.textView.setTextColor(disabledTextColor)
                    } else {
                        viewModel.allowExtensions?.let {
                            if (it.isEmpty() || it.contains(FileUtils.getExtension(item.path))) {
                                binding.textView.setTextColor(primaryTextColor)
                            } else {
                                binding.textView.setTextColor(disabledTextColor)
                            }
                        } ?: binding.textView.setTextColor(primaryTextColor)
                    }
                }
            }
            binding.root.isSelected = item == selectFile
            if (item == selectFile) {
                binding.root.background = selectDrawable
            } else {
                binding.root.setBackgroundColor(getCompatColor(R.color.transparent))
            }
        }

    }

    interface CallBack {
        fun onResult(data: Intent)
    }
}