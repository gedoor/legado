package io.legado.app.ui.main.booksource

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import io.legado.app.R

class BookSourceFragment : Fragment(R.layout.fragment_book_source) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
         Log.e("TAG", "BookSourceFragment")
    }

}