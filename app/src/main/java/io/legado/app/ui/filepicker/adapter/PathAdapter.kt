package io.legado.app.ui.filepicker.adapter

import android.content.Context
import android.os.Environment
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.ui.filepicker.utils.ConvertUtils
import io.legado.app.ui.filepicker.utils.FilePickerIcon
import kotlinx.android.synthetic.main.item_path_filepicker.view.*
import org.jetbrains.anko.sdk27.listeners.onClick
import java.util.*


class PathAdapter(context: Context, val callBack: CallBack) :
    SimpleRecyclerAdapter<String>(context, R.layout.item_path_filepicker) {
    private val paths = LinkedList<String>()
    private val arrowIcon = ConvertUtils.toDrawable(FilePickerIcon.getArrow())

    fun getPath(position: Int): String {
        val tmp = StringBuilder("$sdCardDirectory/")
        //忽略根目录
        if (position == 0) {
            return tmp.toString()
        }
        for (i in 1..position) {
            tmp.append(paths[i]).append("/")
        }
        return tmp.toString()
    }

    fun updatePath(path: String) {
        var path1 = path
        path1 = path1.replace(sdCardDirectory, "")
        paths.clear()
        if (path1 != "/" && path1 != "") {
            val subDirs = path1.substring(path1.indexOf("/") + 1)
                .split("/")
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
            Collections.addAll(paths, *subDirs)
        }
        paths.addFirst(ROOT_HINT)
        setItems(paths)
    }

    override fun convert(holder: ItemViewHolder, item: String, payloads: MutableList<Any>) {
        holder.itemView.apply {
            text_view.text = item
            image_view.setImageDrawable(arrowIcon)
        }
    }

    override fun registerListener(holder: ItemViewHolder) {
        holder.itemView.onClick {
            callBack.onPathClick(holder.layoutPosition)
        }
    }

    interface CallBack {
        fun onPathClick(position: Int)
    }

    companion object {
        private const val ROOT_HINT = "SD"

        @Suppress("DEPRECATION")
        val sdCardDirectory = Environment.getExternalStorageDirectory().absolutePath
    }
}
