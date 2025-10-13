package io.legado.app.help.gsyVideo

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import io.legado.app.R
import io.legado.app.data.entities.BookChapter


class SwitchVideoTypeDialog(private val mContext: Context) : Dialog(
    mContext, R.style.dialog_style
) {
    private var listView: ListView? = null

    private var adapter: ArrayAdapter<BookChapter>? = null

    private var onItemClickListener: OnListItemClickListener? = null

    private var data: List<BookChapter>? = null

    interface OnListItemClickListener {
        fun onItemClick(position: Int)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    fun initList(
        data: List<BookChapter>,
        onItemClickListener: OnListItemClickListener
    ) {
        this.onItemClickListener = onItemClickListener
        this.data = data

        val inflater = LayoutInflater.from(mContext)
        val view: View = inflater.inflate(R.layout.switch_video_dialog, null)
        listView = view.findViewById(R.id.switch_dialog_list)
        setContentView(view)
        adapter = SwitchVideoAdapter(mContext, data)
        listView!!.setAdapter(adapter)
        listView!!.onItemClickListener = this@SwitchVideoTypeDialog.OnItemClickListener()
        val dialogWindow = getWindow()
        val lp = dialogWindow!!.getAttributes()
        val d = mContext.getResources().getDisplayMetrics() // 获取屏幕宽、高用
        lp.width = (d.widthPixels * 0.8).toInt() // 高度设置为屏幕的0.6
        dialogWindow.setAttributes(lp)
    }

    private inner class OnItemClickListener : AdapterView.OnItemClickListener {
        override fun onItemClick(
            adapterView: AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long
        ) {
            dismiss()
            onItemClickListener!!.onItemClick(position)
        }
    }
}