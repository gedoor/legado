package io.legado.app.ui.about

import android.content.Context
import android.os.Bundle
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.ReadRecordShow
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.noButton
import io.legado.app.lib.dialogs.okButton
import io.legado.app.utils.applyTint
import kotlinx.android.synthetic.main.activity_read_record.*
import kotlinx.android.synthetic.main.item_read_record.*
import kotlinx.android.synthetic.main.item_read_record.view.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.sdk27.listeners.onClick

class ReadRecordActivity : BaseActivity(R.layout.activity_read_record) {

    lateinit var adapter: RecordAdapter

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    private fun initView() {
        tv_book_name.setText(R.string.all_read_time)
        adapter = RecordAdapter(this)
        recycler_view.adapter = adapter
        iv_remove.onClick {
            alert(R.string.delete, R.string.sure_del) {
                okButton {
                    App.db.readRecordDao().clear()
                    initData()
                }
                noButton()
            }.show().applyTint()
        }
    }

    private fun initData() {
        launch(IO) {
            val allTime = App.db.readRecordDao().allTime
            withContext(Main) {
                tv_read_time.text = formatDuring(allTime)
            }
            val readRecords = App.db.readRecordDao().allShow
            withContext(Main) {
                adapter.setItems(readRecords)
            }
        }
    }

    inner class RecordAdapter(context: Context) :
        SimpleRecyclerAdapter<ReadRecordShow>(context, R.layout.item_read_record) {

        override fun convert(
            holder: ItemViewHolder,
            item: ReadRecordShow,
            payloads: MutableList<Any>
        ) {
            holder.itemView.apply {
                tv_book_name.text = item.bookName
                tv_read_time.text = formatDuring(item.readTime)
            }
        }

        override fun registerListener(holder: ItemViewHolder) {
            holder.itemView.apply {
                iv_remove.onClick {
                    alert(R.string.delete, R.string.sure_del) {
                        okButton {
                            getItem(holder.layoutPosition)?.let {
                                App.db.readRecordDao().deleteByName(it.bookName)
                                initData()
                            }
                        }
                        noButton()
                    }.show().applyTint()
                }
            }
        }

    }

    fun formatDuring(mss: Long): String {
        val days = mss / (1000 * 60 * 60 * 24)
        val hours = mss % (1000 * 60 * 60 * 24) / (1000 * 60 * 60)
        val minutes = mss % (1000 * 60 * 60) / (1000 * 60)
        val seconds = mss % (1000 * 60) / 1000
        val d = if (days > 0) "${days}天" else ""
        val h = if (hours > 0) "${hours}小时" else ""
        val m = if (minutes > 0) "${minutes}分钟" else ""
        val s = if (seconds > 0) "${seconds}秒" else ""
        return "$d$h$m$s"
    }

}