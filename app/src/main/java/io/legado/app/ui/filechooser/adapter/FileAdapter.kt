package io.legado.app.ui.filechooser.adapter


import android.content.Context
import android.graphics.drawable.Drawable
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.ui.filechooser.FilePickerIcon
import io.legado.app.ui.filechooser.entity.FileItem
import io.legado.app.ui.filechooser.utils.ConvertUtils
import io.legado.app.ui.filechooser.utils.FileUtils
import kotlinx.android.synthetic.main.item_path_filepicker.view.*
import org.jetbrains.anko.sdk27.listeners.onClick
import java.io.File
import java.util.*


class FileAdapter(context: Context, val callBack: CallBack) :
    SimpleRecyclerAdapter<FileItem>(context, R.layout.item_file_filepicker) {
    private var rootPath: String? = null
    var currentPath: String? = null
        private set
    private var allowExtensions: Array<String?>? = null//允许的扩展名
    /**
     * 是否仅仅读取目录
     */
    var isOnlyListDir = false//是否仅仅读取目录
    /**
     * 是否显示返回主目录
     */
    var isShowHomeDir = false//是否显示返回主目录
    /**
     * 是否显示返回上一级
     */
    var isShowUpDir = true//是否显示返回上一级
    /**
     * 是否显示隐藏的目录（以“.”开头）
     */
    var isShowHideDir = true//是否显示隐藏的目录（以“.”开头）
    private var itemHeight = 40// dp
    private var homeIcon: Drawable? = null
    private var upIcon: Drawable? = null
    private var folderIcon: Drawable? = null
    private var fileIcon: Drawable? = null

    fun setFileIcon(fileIcon: Drawable) {
        this.fileIcon = fileIcon
    }

    fun setFolderIcon(folderIcon: Drawable) {
        this.folderIcon = folderIcon
    }

    fun setHomeIcon(homeIcon: Drawable) {
        this.homeIcon = homeIcon
    }

    fun setUpIcon(upIcon: Drawable) {
        this.upIcon = upIcon
    }

    /**
     * 允许的扩展名
     */
    fun setAllowExtensions(allowExtensions: Array<String?>) {
        this.allowExtensions = allowExtensions
    }

    fun setItemHeight(itemHeight: Int) {
        this.itemHeight = itemHeight
    }

    fun loadData(path: String?) {
        if (path == null) {
            return
        }
        if (homeIcon == null) {
            homeIcon = ConvertUtils.toDrawable(FilePickerIcon.getHOME())
        }
        if (upIcon == null) {
            upIcon = ConvertUtils.toDrawable(FilePickerIcon.getUPDIR())
        }
        if (folderIcon == null) {
            folderIcon = ConvertUtils.toDrawable(FilePickerIcon.getFOLDER())
        }
        if (fileIcon == null) {
            fileIcon = ConvertUtils.toDrawable(FilePickerIcon.getFILE())
        }
        val datas = ArrayList<FileItem>()
        if (rootPath == null) {
            rootPath = path
        }
        currentPath = path
        if (isShowHomeDir) {
            //添加“返回主目录”
            val fileRoot = FileItem()
            fileRoot.isDirectory = true
            fileRoot.icon = homeIcon
            fileRoot.name = DIR_ROOT
            fileRoot.size = 0
            fileRoot.path = rootPath ?: path
            datas.add(fileRoot)
        }
        if (isShowUpDir && path != "/") {
            //添加“返回上一级目录”
            val fileParent = FileItem()
            fileParent.isDirectory = true
            fileParent.icon = upIcon
            fileParent.name = DIR_PARENT
            fileParent.size = 0
            fileParent.path = File(path).parent
            datas.add(fileParent)
        }
        currentPath?.let { currentPath ->
            val files: Array<File?>? = allowExtensions?.let { allowExtensions ->
                if (isOnlyListDir) {
                    FileUtils.listDirs(currentPath, allowExtensions)
                } else {
                    FileUtils.listDirsAndFiles(currentPath, allowExtensions)
                }
            } ?: let {
                if (isOnlyListDir) {
                    FileUtils.listDirs(currentPath)
                } else {
                    FileUtils.listDirsAndFiles(currentPath)
                }
            }
            if (files != null) {
                for (file in files) {
                    if (file == null || (!isShowHideDir && file.name.startsWith("."))) {
                        continue
                    }
                    val fileItem = FileItem()
                    val isDirectory = file.isDirectory
                    fileItem.isDirectory = isDirectory
                    if (isDirectory) {
                        fileItem.icon = folderIcon
                        fileItem.size = 0
                    } else {
                        fileItem.icon = fileIcon
                        fileItem.size = file.length()
                    }
                    fileItem.name = file.name
                    fileItem.path = file.absolutePath
                    datas.add(fileItem)
                }
            }
            setItems(datas)
        }

    }

    override fun convert(holder: ItemViewHolder, item: FileItem, payloads: MutableList<Any>) {
        holder.itemView.apply {
            image_view.setImageDrawable(item.icon)
            text_view.text = item.name
            onClick {
                callBack.onFileClick(holder.layoutPosition)
            }
        }
    }

    interface CallBack {
        fun onFileClick(position: Int)
    }

    companion object {
        val DIR_ROOT = "."
        val DIR_PARENT = ".."
    }

}

