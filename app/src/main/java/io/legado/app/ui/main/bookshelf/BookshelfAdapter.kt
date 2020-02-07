package io.legado.app.ui.main.bookshelf

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.legado.app.data.entities.BookGroup
import io.legado.app.ui.main.bookshelf.books.BooksFragment


class BookshelfAdapter(fragment: Fragment, val callBack: CallBack) :
    FragmentStateAdapter(fragment) {

    private val ids = hashSetOf<Long>()

    override fun getItemCount(): Int {
        return callBack.groupSize
    }

    override fun getItemId(position: Int): Long {
        return callBack.getGroup(position).groupId.toLong()
    }

    override fun containsItem(itemId: Long): Boolean {
        return ids.contains(itemId)
    }

    override fun createFragment(position: Int): Fragment {
        val groupId = callBack.getGroup(position).groupId
        ids.add(groupId.toLong())
        return BooksFragment.newInstance(position, groupId)
    }

    interface CallBack {
        val groupSize: Int
        fun getGroup(position: Int): BookGroup
    }
}