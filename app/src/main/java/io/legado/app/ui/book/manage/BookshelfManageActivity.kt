package io.legado.app.ui.book.manage

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppConst
import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookGroup
import io.legado.app.data.entities.BookSource
import io.legado.app.databinding.ActivityArrangeBookBinding
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.book.group.GroupManageDialog
import io.legado.app.ui.book.group.GroupSelectDialog
import io.legado.app.ui.theme.AppTheme
import io.legado.app.ui.widget.SelectActionBar
import io.legado.app.ui.widget.recycler.DragSelectTouchHelper
import io.legado.app.ui.widget.recycler.ItemTouchCallback
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.cnCompare
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.setEdgeEffectColor
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class BookshelfManageActivity :
    VMBaseActivity<ActivityArrangeBookBinding, BookshelfManageViewModel>(),
    PopupMenu.OnMenuItemClickListener,
    SelectActionBar.CallBack,
    BookAdapter.CallBack,
    SourcePickerDialog.Callback,
    GroupSelectDialog.CallBack {

    override val binding by viewBinding(ActivityArrangeBookBinding::inflate)
    override val viewModel by viewModels<BookshelfManageViewModel>()
    override val groupList: ArrayList<BookGroup> = arrayListOf()
    private val groupRequestCode = 22
    private val addToGroupRequestCode = 34
    private val adapter by lazy { BookAdapter(this, this) }
    private var booksFlowJob: Job? = null
    private var menu: Menu? = null
    private var groupId: Long = -1

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        groupId = intent.getLongExtra("groupId", -1)
        launch {
            binding.titleBar.subtitle = withContext(IO) {
                appDb.bookGroupDao.getByID(groupId)?.groupName
                    ?: getString(R.string.no_group)
            }
        }
        initView()
        initGroupData()
        initBookData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.bookshelf_manage, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
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

    override fun onClickSelectBarMainAction() {
        selectGroup(groupRequestCode, 0)
    }

    private fun initView() {
        binding.recyclerView.setEdgeEffectColor(primaryColor)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.addItemDecoration(VerticalDivider(this))
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
        binding.selectActionBar.inflateMenu(R.menu.bookshelf_menage_sel)
        binding.selectActionBar.setOnMenuItemClickListener(this)
        binding.selectActionBar.setCallBack(this)
        binding.composeView.setContent {
            AppTheme {
                BatchChangeSourceDialog(
                    state = viewModel.batchChangeSourceState,
                    size = viewModel.batchChangeSourceSize,
                    position = viewModel.batchChangeSourcePosition
                ) {
                    viewModel.batchChangeSourceCoroutine?.cancel()
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initGroupData() {
        launch {
            appDb.bookGroupDao.flowAll().conflate().collect {
                groupList.clear()
                groupList.addAll(it)
                adapter.notifyDataSetChanged()
                upMenu()
            }
        }
    }

    private fun initBookData() {
        booksFlowJob?.cancel()
        booksFlowJob = launch {
            when (groupId) {
                AppConst.rootGroupId -> appDb.bookDao.flowNoGroup()
                AppConst.bookGroupAllId -> appDb.bookDao.flowAll()
                AppConst.bookGroupLocalId -> appDb.bookDao.flowLocal()
                AppConst.bookGroupAudioId -> appDb.bookDao.flowAudio()
                AppConst.bookGroupNoneId -> appDb.bookDao.flowNoGroup()
                else -> appDb.bookDao.flowByGroup(groupId)
            }.conflate().map { books ->
                when (getPrefInt(PreferKey.bookshelfSort)) {
                    1 -> books.sortedByDescending {
                        it.latestChapterTime
                    }
                    2 -> books.sortedWith { o1, o2 ->
                        o1.name.cnCompare(o2.name)
                    }
                    3 -> books.sortedBy {
                        it.order
                    }
                    else -> books.sortedByDescending {
                        it.durChapterTime
                    }
                }
            }.conflate().collect { books ->
                adapter.setItems(books)
            }
        }
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_group_manage -> showDialogFragment<GroupManageDialog>()
            else -> if (item.groupId == R.id.menu_group) {
                binding.titleBar.subtitle = item.title
                groupId = appDb.bookGroupDao.getByName(item.title.toString())?.groupId ?: 0
                initBookData()
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_del_selection ->
                alert(titleResource = R.string.draw, messageResource = R.string.sure_del) {
                    okButton { viewModel.deleteBook(*adapter.selection.toTypedArray()) }
                    noButton()
                }
            R.id.menu_update_enable ->
                viewModel.upCanUpdate(adapter.selection, true)
            R.id.menu_update_disable ->
                viewModel.upCanUpdate(adapter.selection, false)
            R.id.menu_add_to_group -> selectGroup(addToGroupRequestCode, 0)
            R.id.menu_change_source -> showDialogFragment<SourcePickerDialog>()
            R.id.menu_check_selected_interval -> adapter.checkSelectedInterval()
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
        showDialogFragment(
            GroupSelectDialog(groupId, requestCode)
        )
    }

    override fun upGroup(requestCode: Int, groupId: Long) {
        when (requestCode) {
            groupRequestCode -> adapter.selection.let { books ->
                val array = Array(books.size) {
                    books[it].copy(group = groupId)
                }
                viewModel.updateBook(*array)
            }
            adapter.groupRequestCode -> {
                adapter.actionItem?.let {
                    viewModel.updateBook(it.copy(group = groupId))
                }
            }
            addToGroupRequestCode -> adapter.selection.let { books ->
                val array = Array(books.size) { index ->
                    val book = books[index]
                    book.copy(group = book.group or groupId)
                }
                viewModel.updateBook(*array)
            }
        }
    }

    override fun upSelectCount() {
        binding.selectActionBar.upCountView(adapter.selection.size, adapter.getItems().size)
    }

    override fun updateBook(vararg book: Book) {
        viewModel.updateBook(*book)
    }

    override fun deleteBook(book: Book) {
        alert(titleResource = R.string.draw, messageResource = R.string.sure_del) {
            okButton {
                viewModel.deleteBook(book)
            }
        }
    }

    override fun sourceOnClick(source: BookSource) {
        viewModel.changeSource(adapter.selection, source)
        viewModel.batchChangeSourceState.value = true
    }

}