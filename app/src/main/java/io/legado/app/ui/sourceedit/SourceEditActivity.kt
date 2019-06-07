package io.legado.app.ui.sourceedit

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.data.entities.BookSource
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_source_edit.*

class SourceEditActivity : BaseActivity<SourceEditViewModel>() {
    override val viewModel: SourceEditViewModel
        get() = getViewModel(SourceEditViewModel::class.java)
    override val layoutID: Int
        get() = R.layout.activity_source_edit

    private val adapter = SourceEditAdapter()
    private val sourceEditEntities:ArrayList<SourceEditEntity> = ArrayList()

    override fun onViewModelCreated(viewModel: SourceEditViewModel, savedInstanceState: Bundle?) {
        initRecyclerView()
        viewModel.sourceLiveData.observe(this, Observer {
            upRecyclerView(it)
        })
        if (viewModel.sourceLiveData.value == null) {
            val sourceID = intent.getStringExtra("data")
            if (sourceID == null) {
                upRecyclerView(null)
            } else {
                sourceID.let { viewModel.setBookSource(sourceID) }
            }
        } else {
            upRecyclerView(viewModel.sourceLiveData.value)
        }
    }

    private fun initRecyclerView() {
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.adapter = adapter
    }

    private fun upRecyclerView(bookSource: BookSource?) {
        sourceEditEntities.clear()
        sourceEditEntities.add(SourceEditEntity("origin", bookSource?.origin, R.string.book_source_url))
        sourceEditEntities.add(SourceEditEntity("name", bookSource?.name, R.string.book_source_name))
        sourceEditEntities.add(SourceEditEntity("group", bookSource?.group, R.string.book_source_group))
        adapter.sourceEditEntities = sourceEditEntities
        adapter.notifyDataSetChanged()
    }

    class SourceEditEntity(var key: String, var value:String?, var hint:Int)
}