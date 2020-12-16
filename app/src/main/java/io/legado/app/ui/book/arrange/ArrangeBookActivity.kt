package io.legado.app.ui.book.arrange

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppConst
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookGroup
import io.legado.app.databinding.ActivityArrangeBookBinding
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.ATH
import io.legado.app.ui.book.group.GroupManageDialog
import io.legado.app.ui.book.group.GroupSelectDialog
import io.legado.app.ui.widget.SelectActionBar
import io.legado.app.ui.widget.recycler.DragSelectTouchHelper
import io.legado.app.ui.widget.recycler.ItemTouchCallback
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.cnCompare
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.getViewModel


class ArrangeBookActivity : VMBaseActivity<ActivityArrangeBookBinding, ArrangeBookViewModel>(),
    PopupMenu.OnMenuItemClickListener,
    SelectActionBar.CallBack,
    ArrangeBookAdapter.CallBack,
    GroupSelectDialog.CallBack {
    override val viewModel: ArrangeBookViewModel
        get() = getViewModel(ArrangeBookViewModel::class.java)
    override val groupList: ArrayList<BookGroup> = arrayListOf()
    private val groupRequestCode = 22
    private val addToGroupRequestCode = 34
    private lateinit var adapter: ArrangeBookAdapter
    private var groupLiveData: LiveData<List<BookGroup>>? = null
    private var booksLiveData: LiveData<List<Book>>? = null
    private var menu: Menu? = null
    private var groupId: Long = -1

    override fun getViewBinding(): ActivityArrangeBookBinding {
        return ActivityArrangeBookBinding.inflate(layoutInflater)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        groupId = intent.getLongExtra("groupId", -1)
        binding.titleBar.subtitle = intent.getStringExtra("groupName") ?: getString(R.string.all)
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

    override fun selectAll(selectAll: Boolean) {
        adapter.selectAll(selectAll)
    }

    override fun revertSelection() {
        adapter.revertSelection()
    }

    override fun onClickMainAction() {
        selectGroup(groupRequestCode, 0)
    }

    private fun initView() {
        ATH.applyEdgeEffectColor(binding.recyclerView)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.addItemDecoration(VerticalDivider(this))
        adapter = ArrangeBookAdapter(this, this)
        binding.recyclerView.adapter = adapter
        val itemTouchCallback = ItemTouchCallback(adapter)
        itemTouchCallback.isCanDrag = getPrefInt(PreferKey.bookshelfSort) == 3
        val dragSelectTouchHelper: DragSelectTouchHelper =
            DragSelectTouchHelper(adapter.dragSelectCallback).setSlideArea(16, 50)
        dragSelectTouchHelper.attachToRecyclerView(binding.recyclerView)
        // When this page is opened, it is in selection mode
        dragSelectTouchHelper.activeSlideSelect()
        // Note: need judge selection first, so add ItemTouchHelper after it.
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.recyclerView)
        binding.selectActionBar.setMainActionText(R.string.move_to_group)
        binding.selectActionBar.inflateMenu(R.menu.arrange_book_sel)
        binding.selectActionBar.setOnMenuItemClickListener(this)
        binding.selectActionBar.setCallBack(this)
    }

    private fun initGroupData() {
        groupLiveData?.removeObservers(this)
        groupLiveData = App.db.bookGroupDao.liveDataAll()
        groupLiveData?.observe(this, {
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
                AppConst.bookGroupAllId -> App.db.bookDao.observeAll()
                AppConst.bookGroupLocalId -> App.db.bookDao.observeLocal()
                AppConst.bookGroupAudioId -> App.db.bookDao.observeAudio()
                AppConst.bookGroupNoneId -> App.db.bookDao.observeNoGroup()
                else -> App.db.bookDao.observeByGroup(groupId)
            }
        booksLiveData?.observe(this, { list ->
            val books = when (getPrefInt(PreferKey.bookshelfSort)) {
                1 -> list.sortedByDescending { it.latestChapterTime }
                2 -> list.sortedWith { o1, o2 ->
                    o1.name.cnCompare(o2.name)
                }
                3 -> list.sortedBy { it.order }
                else -> list.sortedByDescending { it.durChapterTime }
            }
            adapter.setItems(books)
        })
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_group_manage -> GroupManageDialog()
                .show(supportFragmentManager, "groupManage")
            else -> if (item.groupId == R.id.menu_group) {
                binding.titleBar.subtitle = item.title
                groupId = App.db.bookGroupDao.getByName(item.title.toString())?.groupId ?: 0
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
                    noButton()
                }.show()
            R.id.menu_update_enable ->
                viewModel.upCanUpdate(adapter.selectedBooks(), true)
            R.id.menu_update_disable ->
                viewModel.upCanUpdate(adapter.selectedBooks(), false)
            R.id.menu_add_to_group -> selectGroup(addToGroupRequestCode, 0)
        }
        return false
    }

    private fun upMenu() {
        menu?.findItem(R.id.menu_book_group)?.subMenu?.let { subMenu ->
            subMenu.removeGroup(R.id.menu_group)
            groupList.forEach { bookGroup ->
                subMenu.add(R.id.menu_group, bookGroup.order, Menu.NONE, bookGroup.groupName)
            }
        }
    }

    override fun selectGroup(requestCode: Int, groupId: Long) {
        GroupSelectDialog.show(supportFragmentManager, groupId, requestCode)
    }

    override fun upGroup(requestCode: Int, groupId: Long) {
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
        binding.selectActionBar.upCountView(adapter.selectedBooks().size, adapter.getItems().size)
    }

    override fun updateBook(vararg book: Book) {
        viewModel.updateBook(*book)
    }

    override fun deleteBook(book: Book) {
        alert(titleResource = R.string.draw, messageResource = R.string.sure_del) {
            okButton {
                viewModel.deleteBook(book)
            }
        }.show()
    }

}