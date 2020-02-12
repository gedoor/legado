package io.legado.app.ui.about

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.ItemViewBinder
import io.legado.app.R

class ActionItemViewBinder(private val context: Context) :
    ItemViewBinder<ActionItem, ActionItemViewBinder.ViewHolder>() {

    override fun onBindViewHolder(holder: ViewHolder, item: ActionItem) {
        val text = item.text
        val textRes = item.textRes

        holder.text.visibility = View.VISIBLE
        when {
            text != null -> {
                holder.text.text = text
                holder.text.setLineSpacing(0f, holder.text.lineSpacingMultiplier)
            }
            textRes != 0 -> {
                holder.text.setText(textRes)
                holder.text.setLineSpacing(0f, holder.text.lineSpacingMultiplier)
            }
            else -> holder.text.visibility = GONE
        }

        val subText = item.subText
        val subTextRes = item.subTextRes

        holder.subText.visibility = View.VISIBLE
        when {
            subText != null -> holder.subText.text = subText
            subTextRes != 0 -> holder.subText.setText(subTextRes)
            else -> holder.subText.visibility = GONE
        }

        if (item.shouldShowIcon()) {
            holder.icon.visibility = View.VISIBLE
            val drawable = item.icon
            val drawableRes = item.iconRes
            if (drawable != null) {
                holder.icon.setImageDrawable(drawable)
            } else if (drawableRes != 0) {
                holder.icon.setImageResource(drawableRes)
            }
        } else {
            holder.icon.visibility = GONE
        }

        if (item.onClickAction != null || item.onLongClickAction != null) {
            val outValue = TypedValue()
            context.theme.resolveAttribute(
                R.attr.selectableItemBackground,
                outValue,
                true
            )
            holder.view.setBackgroundResource(outValue.resourceId)
        } else {
            holder.view.setBackgroundResource(0)
        }
        holder.setOnClickAction(item.onClickAction)
        holder.setOnLongClickAction(item.onLongClickAction)
    }

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
        return ViewHolder(
            inflater.inflate(R.layout.about_page_action_item, parent, false)
        )
    }

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view), View.OnClickListener,
        View.OnLongClickListener {
        val icon: ImageView = view.findViewById(R.id.mal_item_image)
        val text: TextView = view.findViewById(R.id.mal_item_text)
        val subText: TextView = view.findViewById(R.id.mal_action_item_subtext)
        private var onClickAction: ActionListener? = null
        private var onLongClickAction: ActionListener? = null

        fun setOnClickAction(onClickAction: ActionListener?) {
            this.onClickAction = onClickAction
            view.setOnClickListener(if (onClickAction != null) this else null)
        }

        fun setOnLongClickAction(onLongClickAction: ActionListener?) {
            this.onLongClickAction = onLongClickAction
            view.setOnLongClickListener(if (onLongClickAction != null) this else null)
        }

        override fun onClick(v: View) {
            if (onClickAction != null) {
                onClickAction!!.action()
            }
        }

        override fun onLongClick(v: View): Boolean {
            if (onLongClickAction != null) {
                onLongClickAction!!.action()
                return true
            }
            return false
        }
    }
}
