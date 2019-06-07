package io.legado.app.ui.sourceedit

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_source_edit.*

class SourceEditActivity : BaseActivity<SourceEditViewModel>() {
    override val viewModel: SourceEditViewModel
        get() = getViewModel(SourceEditViewModel::class.java)
    override val layoutID: Int
        get() = R.layout.activity_source_edit

    val adapter = SourceEditAdapter()

    override fun onViewModelCreated(viewModel: SourceEditViewModel, savedInstanceState: Bundle?) {
        val sourceID = intent.getStringExtra("data")
        sourceID?.let { viewModel.setBookSource(sourceID)}
        initRecyclerView()
    }

    fun initRecyclerView() {
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.adapter = adapter
    }
}