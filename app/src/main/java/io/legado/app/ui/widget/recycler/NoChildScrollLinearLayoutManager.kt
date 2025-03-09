package io.legado.app.ui.widget.recycler

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class NoChildScrollLinearLayoutManager(context: Context) : LinearLayoutManager(context) {

    override fun requestChildRectangleOnScreen(
        parent: RecyclerView,
        child: View,
        rect: Rect,
        immediate: Boolean,
        focusedChildVisible: Boolean
    ): Boolean {
        return false
    }

    override fun requestChildRectangleOnScreen(
        parent: RecyclerView,
        child: View,
        rect: Rect,
        immediate: Boolean
    ): Boolean {
        return false
    }

}
