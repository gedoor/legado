package io.legado.app.ui.about

import android.content.Context
import android.os.Bundle
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.ReadRecordShow
import io.legado.app.databinding.ActivityReadRecordBinding
import io.legado.app.databinding.ItemReadRecordBinding
import io.legado.app.lib.dialogs.alert
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.sdk27.listeners.onClick

class ReadRecordActivity : BaseActivity<ActivityReadRecordBinding>() {

    lateinit var adapter: RecordAdapter

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    private fun initView() {
        binding.readRecord.tvBookName.setText(R.string.all_read_time)
        adapter = RecordAdapter(this)
        binding.recyclerView.adapter = adapter
        binding.readRecord.ivRemove.onClick {
            alert(R.string.delete, R.string.sure_del) {
                okButton {
                    App.db.readRecordDao().clear()
                    initData()
                }
                noButton()
            }.show()
        }
    }

    private fun initData() {
        launch(IO) {
            val allTime = App.db.readRecordDao().allTime
            withContext(Main) {
                binding.readRecord.tvReadTime.text = formatDuring(allTime)
            }
            val readRecords = App.db.readRecordDao().allShow
            withContext(Main) {
                adapter.setItems(readRecords)
            }
        }
    }

    inner class RecordAdapter(context: Context) :
        SimpleRecyclerAdapter<ReadRecordShow, ItemReadRecordBinding>(context) {

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemReadRecordBinding,
            item: ReadRecordShow,
            payloads: MutableList<Any>
        ) {
            binding.apply {
                tvBookName.text = item.bookName
                tvReadTime.text = formatDuring(item.readTime)
            }
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemReadRecordBinding) {
            binding.apply {
                ivRemove.onClick {
                    alert(R.string.delete, R.string.sure_del) {
                        okButton {
                            getItem(holder.layoutPosition)?.let {
                                App.db.readRecordDao().deleteByName(it.bookName)
                                initData()
                            }
                        }
                        noButton()
                    }.show()
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