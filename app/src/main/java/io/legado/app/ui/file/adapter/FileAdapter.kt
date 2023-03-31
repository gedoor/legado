package io.legado.app.ui.file.adapter


import android.content.Context
import android.view.ViewGroup
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.databinding.ItemFilePickerBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.theme.getPrimaryDisabledTextColor
import io.legado.app.lib.theme.getPrimaryTextColor
import io.legado.app.ui.file.entity.FileItem
import io.legado.app.ui.file.utils.FilePickerIcon
import io.legado.app.utils.ConvertUtils
import io.legado.app.utils.FileUtils
import java.io.File


class FileAdapter(context: Context, val callBack: CallBack) :
    RecyclerAdapter<FileItem, ItemFilePickerBinding>(context) {
    private var rootPath: String? = null
    var currentPath: String? = null
        private set
    private val homeIcon = ConvertUtils.toDrawable(FilePickerIcon.getHome())!!
    private val upIcon = ConvertUtils.toDrawable(FilePickerIcon.getUpDir())!!
    private val folderIcon = ConvertUtils.toDrawable(FilePickerIcon.getFolder())!!
    private val fileIcon = ConvertUtils.toDrawable(FilePickerIcon.getFile())!!
    private val primaryTextColor = context.getPrimaryTextColor(!AppConfig.isNightTheme)
    private val disabledTextColor = context.getPrimaryDisabledTextColor(!AppConfig.isNightTheme)
    private val dirRoot = "."
    private val dirParent = ".."

    fun loadData(path: String?) {
        if (path == null) {
            return
        }
        val data = ArrayList<FileItem>()
        if (rootPath == null) {
            rootPath = path
        }
        currentPath = path
        if (callBack.isShowHomeDir) {
            //添加“返回主目录”
            val fileRoot = FileItem(
                isDirectory = true,
                icon = homeIcon,
                name = dirRoot,
                path = rootPath ?: path
            )
            data.add(fileRoot)
        }
        if (callBack.isShowUpDir && path != PathAdapter.sdCardDirectory) {
            //添加“返回上一级目录”
            val fileParent = FileItem(
                isDirectory = true,
                icon = upIcon,
                name = dirParent,
                path = File(path).parent ?: ""
            )
            data.add(fileParent)
        }
        currentPath?.let { currentPath ->
            val files: Array<File>? = FileUtils.listDirsAndFiles(currentPath)
            if (files != null) {
                for (file in files) {
                    if (!callBack.isShowHideDir && file.name.startsWith(".")) {
                        continue
                    }
                    val fileItem = if (file.isDirectory) {
                        FileItem(
                            name = file.name,
                            icon = folderIcon,
                            path = file.absolutePath,
                            isDirectory = true
                        )
                    } else {
                        FileItem(
                            name = file.name,
                            icon = fileIcon,
                            path = file.absolutePath,
                            size = file.length(),
                            isDirectory = true
                        )
                    }
                    data.add(fileItem)
                }
            }
            setItems(data)
        }

    }

    override fun getViewBinding(parent: ViewGroup): ItemFilePickerBinding {
        return ItemFilePickerBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemFilePickerBinding,
        item: FileItem,
        payloads: MutableList<Any>
    ) {
        binding.apply {
            imageView.setImageDrawable(item.icon)
            textView.text = item.name
            if (item.isDirectory) {
                textView.setTextColor(primaryTextColor)
            } else {
                if (callBack.isSelectDir) {
                    textView.setTextColor(disabledTextColor)
                } else {
                    callBack.allowExtensions?.let {
                        if (it.isEmpty() || it.contains(FileUtils.getExtension(item.path))) {
                            textView.setTextColor(primaryTextColor)
                        } else {
                            textView.setTextColor(disabledTextColor)
                        }
                    } ?: textView.setTextColor(primaryTextColor)
                }
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemFilePickerBinding) {
        holder.itemView.setOnClickListener {
            callBack.onFileClick(holder.layoutPosition)
        }
    }

    interface CallBack {
        fun onFileClick(position: Int)

        //允许的扩展名
        var allowExtensions: Array<String>?

        /**
         * 是否选取目录
         */
        val isSelectDir: Boolean

        /**
         * 是否显示返回主目录
         */
        var isShowHomeDir: Boolean

        /**
         * 是否显示返回上一级
         */
        var isShowUpDir: Boolean

        /**
         * 是否显示隐藏的目录（以“.”开头）
         */
        var isShowHideDir: Boolean
    }

}

