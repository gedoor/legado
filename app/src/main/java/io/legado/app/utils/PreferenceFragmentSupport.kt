package io.legado.app.utils

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.setPadding
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceRecyclerViewAccessibilityDelegate
import androidx.recyclerview.widget.RecyclerView
import fadeapp.widgets.ScrollLessRecyclerView
import io.legado.app.R
import io.legado.app.help.AppConfig

abstract class PreferenceFragmentSupport : PreferenceFragmentCompat(){

    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?
    ): RecyclerView {

        if (context?.packageManager?.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)==true) {
            val recyclerView: RecyclerView?= parent.findViewById(R.id.recycler_view)
            if (recyclerView != null) {
                return recyclerView
            }
        }
        val recyclerView = ScrollLessRecyclerView(inflater.context,null,R.attr.preferenceFragmentListStyle)
        recyclerView.id = R.id.recycler_view
        recyclerView.setPadding(0)
        recyclerView.clipToPadding = false
        recyclerView.isEnableScroll = !AppConfig.isEInkMode
        recyclerView.layoutManager = onCreateLayoutManager()
        recyclerView.setAccessibilityDelegateCompat(
            PreferenceRecyclerViewAccessibilityDelegate(recyclerView)
        )

        return recyclerView
    }
}