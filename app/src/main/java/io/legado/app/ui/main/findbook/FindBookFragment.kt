package io.legado.app.ui.main.findbook

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import io.legado.app.R

class FindBookFragment : Fragment(R.layout.fragment_find_book) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.e("TAG", "FindBookFragment")
    }

}