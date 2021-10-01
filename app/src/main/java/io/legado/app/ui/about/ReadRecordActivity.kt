package io.legado.app.ui.about

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.data.appDb
import io.legado.app.data.entities.ReadRecordShow
import io.legado.app.databinding.ActivityReadRecordBinding
import io.legado.app.databinding.ItemReadRecordBinding
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.book.search.SearchActivity
import io.legado.app.utils.cnCompare
import io.legado.app.utils.startActivity
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class ReadRecordActivity : BaseActivity<ActivityReadRecordBinding>() {

    private val adapter by lazy { RecordAdapter(this) }
    private var sortMode = 0

    override val binding by viewBinding(ActivityReadRecordBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_read_record, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_name -> {
                sortMode = 0
                initData()
            }
            R.id.menu_sort_time -> {
                sortMode = 1
                initData()
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() = binding.run {
        readRecord.tvBookName.setText(R.string.all_read_time)
        recyclerView.adapter = adapter
        readRecord.tvRemove.setOnClickListener {
            alert(R.string.delete, R.string.sure_del) {
                okButton {
                    appDb.readRecordDao.clear()
                    initData()
                }
                noButton()
            }
        }
    }

    private fun initData() {
        launch(IO) {
            val allTime = appDb.readRecordDao.allTime
            withContext(Main) {
                binding.readRecord.tvReadTime.text = formatDuring(allTime)
            }
            var readRecords = appDb.readRecordDao.allShow
            readRecords = when (sortMode) {
                1 -> readRecords.sortedBy { it.readTime }
                else -> {
                    readRecords.sortedWith { o1, o2 ->
                        o1.bookName.cnCompare(o2.bookName)
                    }
                }
            }
            withContext(Main) {
                adapter.setItems(readRecords)
            }
        }
    }

    inner class RecordAdapter(context: Context) :
        RecyclerAdapter<ReadRecordShow, ItemReadRecordBinding>(context) {

        override fun getViewBinding(parent: ViewGroup): ItemReadRecordBinding {
            return ItemReadRecordBinding.inflate(inflater, parent, false)
        }

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
                root.setOnClickListener {
                    val item = getItem(holder.layoutPosition) ?: return@setOnClickListener
                    launch {
                        val book = withContext(IO) {
                            appDb.bookDao.findByName(item.bookName).firstOrNull()
                        }
                        if (book == null) {
                            SearchActivity.start(this@ReadRecordActivity, item.bookName)
                        } else {
                            startActivity<ReadBookActivity> {
                                putExtra("bookUrl", book.bookUrl)
                            }
                        }
                    }
                }
                tvRemove.setOnClickListener {
                    getItem(holder.layoutPosition)?.let { item ->
                        sureDelAlert(item)
                    }
                }
            }
        }

        private fun sureDelAlert(item: ReadRecordShow) {
            alert(R.string.delete) {
                setMessage(getString(R.string.sure_del_any, item.bookName))
                okButton {
                    appDb.readRecordDao.deleteByName(item.bookName)
                    initData()
                }
                noButton()
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
        var time = "$d$h$m$s"
        if (time.isBlank()) {
            time = "0秒"
        }
        return time
    }

}