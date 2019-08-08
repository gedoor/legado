package io.legado.app.ui.chapterlist

import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.utils.getViewModelOfActivity

class BookmarkFragment : VMBaseFragment<ChapterListViewModel>(R.layout.fragment_bookmark) {
    override val viewModel: ChapterListViewModel
        get() = getViewModelOfActivity(ChapterListViewModel::class.java)


}