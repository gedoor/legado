package io.legado.app.ui.main.rss

import android.os.Bundle
import android.view.View
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.ThemeStore
import kotlinx.android.synthetic.main.fragment_rss.*

class RssFragment : BaseFragment(R.layout.fragment_rss) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initRecyclerView()
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(recycler_view)
    }
}