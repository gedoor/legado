package io.legado.app.ui.book.arrange

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppConst
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookGroup
import io.legado.app.help.AppConfig
import io.legado.app.help.ItemTouchCallback
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.noButton
import io.legado.app.lib.dialogs.okButton
import io.legado.app.lib.theme.ATH
import io.legado.app.ui.book.group.GroupManageDialog
import io.legado.app.ui.book.group.GroupSelectDialog
import io.legado.app.ui.widget.SelectActionBar
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.applyTint
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_arrange_book.*


class ArrangeBookActivity : VMBaseActivity<ArrangeBookViewModel>(R.layout.activity_arrange_book),
    PopupMenu.OnMenuItemClickListener,
    ArrangeBookAdapter.CallBack, GroupSelectDialog.CallBack {
    override val viewModel: ArrangeBookViewModel
        get() = getViewModel(ArrangeBookViewModel::class.java)
    override val groupList: ArrayList<BookGroup> = arrayListOf()
    private val groupRequestCode = 22
    private val addToGroupRequestCode = 34
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

    private fun initView() {
        ATH.applyEdgeEffectColor(recycler_view)
        recycler_view.isEnableScroll = !AppConfig.isEInkMode
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.addItemDecoration(VerticalDivider(this))
        adapter = ArrangeBookAdapter(this, this)
        recycler_view.adapter = adapter
        val itemTouchCallback = ItemTouchCallback()
        itemTouchCallback.onItemTouchCallbackListener = adapter
        itemTouchCallback.isCanDrag = getPrefInt(PreferKey.bookshelfSort) == 3
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(recycler_view)
        select_action_bar.setMainActionText(R.string.move_to_group)
        select_action_bar.inflateMenu(R.menu.arrange_book_sel)
        select_action_bar.setOnMenuItemClickListener(this)
        select_action_bar.setCallBack(object : SelectActionBar.CallBack {
            override fun selectAll(selectAll: Boolean) {
                adapter.selectAll(selectAll)
            }

            override fun revertSelection() {
                adapter.revertSelection()
            }

            override fun onClickMainAction() {
                selectGroup(0, groupRequestCode)
            }
        })
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
            when (groupId) {
                AppConst.bookGroupAll.groupId -> App.db.bookDao().observeAll()
                AppConst.bookGroupLocal.groupId -> App.db.bookDao().observeLocal()
                AppConst.bookGroupAudio.groupId -> App.db.bookDao().observeAudio()
                AppConst.bookGroupNone.groupId -> App.db.bookDao().observeNoGroup()
                else -> App.db.bookDao().observeByGroup(groupId)
            }
        booksLiveData?.observe(this, Observer { list ->
            val books = when (getPrefInt(PreferKey.bookshelfSort)) {
                1 -> list.sortedByDescending { it.latestChapterTime }
                2 -> list.sortedBy { it.name }
                3 -> list.sortedBy { it.order }
                else -> list.sortedByDescending { it.durChapterTime }
            }
            adapter.setItems(books)
            upSelectCount()
        })
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_group_manage -> GroupManageDialog()
                .show(supportFragmentManager, "groupManage")
            R.id.menu_no_group -> {
                title_bar.subtitle = getString(R.string.no_group)
                groupId = AppConst.bookGroupNone.groupId
                initBookData()
            }
            R.id.menu_all -> {
                title_bar.subtitle = item.title
                groupId = AppConst.bookGroupAll.groupId
                initBookData()
            }
            R.id.menu_local -> {
                title_bar.subtitle = item.title
                groupId = AppConst.bookGroupLocal.groupId
                initBookData()
            }
            R.id.menu_audio -> {
                title_bar.subtitle = item.title
                groupId = AppConst.bookGroupAudio.groupId
                initBookData()
            }
            else -> if (item.groupId == R.id.menu_group) {
                title_bar.subtitle = item.title
                groupId = item.itemId
                initBookData()
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_del_selection ->
                alert(titleResource = R.string.draw, messageResource = R.string.sure_del) {
                    okButton { viewModel.deleteBook(*adapter.selectedBooks()) }
                    noButton { }
                }.show().applyTint()
            R.id.menu_update_enable ->
                viewModel.upCanUpdate(adapter.selectedBooks(), true)
            R.id.menu_update_disable ->
                viewModel.upCanUpdate(adapter.selectedBooks(), false)
            R.id.menu_add_to_group -> selectGroup(0, addToGroupRequestCode)
        }
        return false
    }

    private fun upMenu() {
        menu?.findItem(R.id.menu_book_group)?.subMenu?.let { subMenu ->
            subMenu.removeGroup(R.id.menu_group)
            groupList.forEach { bookGroup ->
                subMenu.add(R.id.menu_group, bookGroup.groupId, Menu.NONE, bookGroup.groupName)
            }
        }
    }

    override fun selectGroup(groupId: Int, requestCode: Int) {
        GroupSelectDialog.show(supportFragmentManager, groupId, requestCode)
    }

    override fun upGroup(requestCode: Int, groupId: Int) {
        when (requestCode) {
            groupRequestCode -> {
                val books = arrayListOf<Book>()
                adapter.selectedBooks().forEach {
                    books.add(it.copy(group = groupId))
                }
                viewModel.updateBook(*books.toTypedArray())
            }
            adapter.groupRequestCode -> {
                adapter.actionItem?.let {
                    viewModel.updateBook(it.copy(group = groupId))
                }
            }
            addToGroupRequestCode -> {
                val books = arrayListOf<Book>()
                adapter.selectedBooks().forEach {
                    books.add(it.copy(group = it.group or groupId))
                }
                viewModel.updateBook(*books.toTypedArray())
            }
        }
    }

    override fun upSelectCount() {
        select_action_bar.upCountView(adapter.selectedBooks().size, adapter.getItems().size)
    }

    override fun updateBook(vararg book: Book) {
        viewModel.updateBook(*book)
    }

    override fun deleteBook(book: Book) {
        alert(titleResource = R.string.draw, messageResource = R.string.sure_del) {
            okButton {
                viewModel.deleteBook(book)
            }
        }.show().applyTint()
    }

}