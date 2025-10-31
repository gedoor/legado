package io.legado.app.help.gsyVideo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import io.legado.app.R
import io.legado.app.data.entities.BookChapter

class SwitchVideoAdapter(
    context: Context,
    private val dataList: List<BookChapter>
) : ArrayAdapter<BookChapter>(context, 0, dataList) {
    override fun getCount(): Int = dataList.size

    override fun getItem(position: Int): BookChapter = dataList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.switch_video_dialog_item, parent, false)
        val textView = view.findViewById<TextView>(R.id.text1)
        val item = dataList[position]
        textView.text = item.title
        return view
    }
}