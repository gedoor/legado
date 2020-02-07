package io.legado.app.ui.book.arrange

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookGroup
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.okButton
import io.legado.app.lib.theme.ATH
import io.legado.app.ui.book.group.GroupSelectDialog
import io.legado.app.utils.applyTint
import io.legado.app.utils.getVerticalDivider
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_arrange_book.*
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.toast


class ArrangeBookActivity : VMBaseActivity<ArrangeBookViewModel>(R.layout.activity_arrange_book),
    ArrangeBookAdapter.CallBack, GroupSelectDialog.CallBack {
    override val viewModel: ArrangeBookViewModel
        get() = getViewModel(ArrangeBookViewModel::class.java)
    override val groupList: ArrayList<BookGroup> = arrayListOf()
    private val groupRequestCode = 22
    private lateinit var adapter: ArrangeBookAdapter
    private var groupLiveData: LiveData<List<BookGroup>>? = null
    private var booksLiveData: LiveData<List<Book>>? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.arrange_book, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() {
        ATH.applyEdgeEffectColor(recycler_view)
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.addItemDecoration(recycler_view.getVerticalDivider())
        adapter = ArrangeBookAdapter(this, this)
        recycler_view.adapter = adapter
        cb_selected_all.onClick {
            adapter.selectAll(!adapter.isSelectAll())
        }
        btn_delete.onClick {
            if (adapter.selectedBooks.isEmpty()) {
                toast(R.string.non_select)
                return@onClick
            }
            alert(titleResource = R.string.sure, messageResource = R.string.sure_del) {
                okButton {
                    viewModel.deleteBook(*adapter.selectedBooks.toTypedArray())
                }
            }.show().applyTint()
        }
        btn_to_group.onClick {
            if (adapter.selectedBooks.isEmpty()) {
                toast(R.string.non_select)
                return@onClick
            }
            selectGroup(groupRequestCode)
        }
    }

    private fun initData() {
        groupLiveData?.removeObservers(this)
        groupLiveData = App.db.bookGroupDao().liveDataAll()
        groupLiveData?.observe(this, Observer {
            groupList.clear()
            groupList.addAll(it)
            adapter.notifyDataSetChanged()
        })
        booksLiveData?.removeObservers(this)
        booksLiveData = App.db.bookDao().observeAll()
        booksLiveData?.observe(this, Observer {
            adapter.setItems(it)
            upSelectCount()
        })
    }

    override fun selectGroup(requestCode: Int) {
        GroupSelectDialog.show(supportFragmentManager, requestCode)
    }

    override fun upGroup(requestCode: Int, group: BookGroup) {
        when (requestCode) {
            groupRequestCode -> {
                val books = arrayListOf<Book>()
                adapter.selectedBooks.forEach {
                    books.add(it.copy(group = group.groupId))
                }
                viewModel.updateBook(*books.toTypedArray())
            }
            adapter.groupRequestCode -> {
                adapter.actionItem?.let {
                    viewModel.updateBook(it.copy(group = group.groupId))
                }
            }
        }
    }

    override fun upSelectCount() {
        cb_selected_all.isChecked = adapter.isSelectAll()
        //重置全选的文字
        if (cb_selected_all.isChecked) {
            cb_selected_all.setText(R.string.cancel)
        } else {
            cb_selected_all.setText(R.string.select_all)
        }
        tv_select_count.text =
            getString(R.string.select_count, adapter.selectedBooks.size, adapter.getItems().size)
    }

    override fun deleteBook(book: Book) {
        alert(titleResource = R.string.sure, messageResource = R.string.sure_del) {
            okButton {
                viewModel.deleteBook(book)
            }
        }.show().applyTint()
    }
}