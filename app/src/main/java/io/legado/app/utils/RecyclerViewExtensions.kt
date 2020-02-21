package io.legado.app.utils

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R


fun RecyclerView.getVerticalDivider(): DividerItemDecoration {
    return DividerItemDecoration(context, DividerItemDecoration.VERTICAL).apply {
        ContextCompat.getDrawable(context, R.drawable.ic_divider)?.let {
            this.setDrawable(it)
        }
    }
}