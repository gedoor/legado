package io.legado.app.ui.about

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.ItemViewBinder
import io.legado.app.R
import io.legado.app.lib.theme.ColorUtils
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.backgroundColor
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.textColor

class CategoryViewBinder : ItemViewBinder<Category, CategoryViewBinder.ViewHolder>() {

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
        return ViewHolder(inflater.inflate(R.layout.about_page_item_category, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, item: Category) {
        val title = item.title
        val titleRes = item.titleRes
        when {
            title != null -> holder.category.text = title
            titleRes != 0 -> holder.category.setText(titleRes)
        }
        holder.category.text = item.title
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var category: TextView = itemView.findViewById(R.id.category)

        init {
            category.textColor = itemView.context.accentColor
            itemView.backgroundColor = ColorUtils.darkenColor(itemView.context.backgroundColor)
        }
    }
}
