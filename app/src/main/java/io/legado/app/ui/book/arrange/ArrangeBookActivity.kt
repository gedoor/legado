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
import io.legado.app.ui.book.group.GroupManageDialog
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
    private var menu: Menu? = null
    private var groupId: Int = -1

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        groupId = intent.getIntExtra("groupId", -1)
        title_bar.subtitle = intent.getStringExtra("groupName") ?: getString(R.string.all)
        initView()
        initGroupData()
        initBookData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.arrange_book, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        this.menu = menu
        upMenu()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_group_manage -> GroupManageDialog()
                .show(supportFragmentManager, "groupManage")
            R.id.menu_all -> {
                title_bar.subtitle = item.title
                groupId = -1
                adapter.selectedBooks.clear()
                initBookData()
            }
            R.id.menu_local -> {
                title_bar.subtitle = item.title
                groupId = -2
                adapter.selectedBooks.clear()
                initBookData()
            }
            R.id.menu_audio -> {
                title_bar.subtitle = item.title
                groupId = -3
                adapter.selectedBooks.clear()
                initBookData()
            }
            else -> if (item.groupId == R.id.menu_group) {
                title_bar.subtitle = item.title
                groupId = item.itemId
                adapter.selectedBooks.clear()
                initBookData()
            }
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

    private fun initGroupData() {
        groupLiveData?.removeObservers(this)
        groupLiveData = App.db.bookGroupDao().liveDataAll()
        groupLiveData?.observe(this, Observer {
            groupList.clear()
            groupList.addAll(it)
            adapter.notifyDataSetChanged()
            upMenu()
        })
    }

    private fun initBookData() {
        booksLiveData?.removeObservers(this)
        booksLiveData =
            if (groupId == -1) {
                App.db.bookDao().observeAll()
            } else {
                App.db.bookDao().observeByGroup(groupId)
            }
        booksLiveData?.observe(this, Observer {
            adapter.setItems(it)
            upSelectCount()
        })
    }

    private fun upMenu() {
        menu?.findItem(R.id.menu_book_group)?.subMenu?.let { subMenu ->
            subMenu.removeGroup(R.id.menu_group)
            groupList.forEach { bookGroup ->
                subMenu.add(R.id.menu_group, bookGroup.groupId, Menu.NONE, bookGroup.groupName)
            }
        }
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
            cb_selected_all.text = getString(
                R.string.select_cancel_count,
                adapter.selectedBooks.size,
                adapter.getItems().size
            )
        } else {
            cb_selected_all.text = getString(
                R.string.select_all_count,
                adapter.selectedBooks.size,
                adapter.getItems().size
            )
        }
        setMenuClickable(adapter.selectedBooks.isNotEmpty())
    }

    private fun setMenuClickable(isClickable: Boolean) {
        //设置是否可删除
        btn_delete.isEnabled = isClickable
        btn_delete.isClickable = isClickable
        //设置是否可添加书籍
        btn_to_group.isEnabled = isClickable
        btn_to_group.isClickable = isClickable
    }

    override fun deleteBook(book: Book) {
        alert(titleResource = R.string.sure, messageResource = R.string.sure_del) {
            okButton {
                viewModel.deleteBook(book)
            }
        }.show().applyTint()
    }
}