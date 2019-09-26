package io.legado.app.ui.main.bookshelf

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter


class BookshelfAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        return BooksFragment()
    }

}