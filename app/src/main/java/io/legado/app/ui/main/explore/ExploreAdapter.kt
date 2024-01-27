package io.legado.app.ui.main.explore

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.view.children
import com.google.android.flexbox.FlexboxLayout
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.data.entities.BookSourcePart
import io.legado.app.data.entities.rule.ExploreKind
import io.legado.app.databinding.ItemFilletTextBinding
import io.legado.app.databinding.ItemFindBookBinding
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.source.clearExploreKindsCache
import io.legado.app.help.source.exploreKinds
import io.legado.app.lib.theme.accentColor
import io.legado.app.ui.login.SourceLoginActivity
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.activity
import io.legado.app.utils.dpToPx
import io.legado.app.utils.gone
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.startActivity
import io.legado.app.utils.visible
import kotlinx.coroutines.CoroutineScope
import splitties.views.onLongClick

class ExploreAdapter(context: Context, val callBack: CallBack) :
    RecyclerAdapter<BookSourcePart, ItemFindBookBinding>(context) {

    private val recycler = arrayListOf<View>()
    private var exIndex = -1
    private var scrollTo = -1

    override fun getViewBinding(parent: ViewGroup): ItemFindBookBinding {
        return ItemFindBookBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemFindBookBinding,
        item: BookSourcePart,
        payloads: MutableList<Any>
    ) {
        binding.run {
            if (holder.layoutPosition == itemCount - 1) {
                root.setPadding(16.dpToPx(), 12.dpToPx(), 16.dpToPx(), 12.dpToPx())
            } else {
                root.setPadding(16.dpToPx(), 12.dpToPx(), 16.dpToPx(), 0)
            }
            if (payloads.isEmpty()) {
                tvName.text = item.bookSourceName
            }
            if (exIndex == holder.layoutPosition) {
                ivStatus.setImageResource(R.drawable.ic_arrow_down)
                rotateLoading.loadingColor = context.accentColor
                rotateLoading.visible()
                if (scrollTo >= 0) {
                    callBack.scrollTo(scrollTo)
                }
                Coroutine.async(callBack.scope) {
                    item.exploreKinds()
                }.onSuccess { kindList ->
                    upKindList(flexbox, item.bookSourceUrl, kindList)
                }.onFinally {
                    rotateLoading.gone()
                    if (scrollTo >= 0) {
                        callBack.scrollTo(scrollTo)
                        scrollTo = -1
                    }
                }
            } else kotlin.runCatching {
                ivStatus.setImageResource(R.drawable.ic_arrow_right)
                rotateLoading.gone()
                recyclerFlexbox(flexbox)
                flexbox.gone()
            }
        }
    }

    private fun upKindList(flexbox: FlexboxLayout, sourceUrl: String, kinds: List<ExploreKind>) {
        if (kinds.isNotEmpty()) kotlin.runCatching {
            recyclerFlexbox(flexbox)
            flexbox.visible()
            kinds.forEach { kind ->
                val tv = getFlexboxChild(flexbox)
                flexbox.addView(tv)
                tv.text = kind.title
                val lp = tv.layoutParams as FlexboxLayout.LayoutParams
                kind.style().let { style ->
                    lp.flexGrow = style.layout_flexGrow
                    lp.flexShrink = style.layout_flexShrink
                    lp.alignSelf = style.alignSelf()
                    lp.flexBasisPercent = style.layout_flexBasisPercent
                    lp.isWrapBefore = style.layout_wrapBefore
                }
                if (kind.url.isNullOrBlank()) {
                    tv.setOnClickListener(null)
                } else {
                    tv.setOnClickListener {
                        if (kind.title.startsWith("ERROR:")) {
                            it.activity?.showDialogFragment(TextDialog("ERROR", kind.url))
                        } else {
                            callBack.openExplore(sourceUrl, kind.title, kind.url)
                        }
                    }
                }
            }
        }
    }

    @Synchronized
    private fun getFlexboxChild(flexbox: FlexboxLayout): TextView {
        return if (recycler.isEmpty()) {
            ItemFilletTextBinding.inflate(inflater, flexbox, false).root
        } else {
            recycler.last().also {
                recycler.removeLast()
            } as TextView
        }
    }

    @Synchronized
    private fun recyclerFlexbox(flexbox: FlexboxLayout) {
        recycler.addAll(flexbox.children)
        flexbox.removeAllViews()
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemFindBookBinding) {
        binding.apply {
            llTitle.setOnClickListener {
                val position = holder.layoutPosition
                val oldEx = exIndex
                exIndex = if (exIndex == position) -1 else position
                notifyItemChanged(oldEx, false)
                if (exIndex != -1) {
                    scrollTo = position
                    callBack.scrollTo(position)
                    notifyItemChanged(position, false)
                }
            }
            llTitle.onLongClick {
                showMenu(llTitle, holder.layoutPosition)
            }
        }
    }

    fun compressExplore(): Boolean {
        return if (exIndex < 0) {
            false
        } else {
            val oldExIndex = exIndex
            exIndex = -1
            notifyItemChanged(oldExIndex)
            true
        }
    }

    private fun showMenu(view: View, position: Int): Boolean {
        val source = getItem(position) ?: return true
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.explore_item)
        popupMenu.menu.findItem(R.id.menu_login).isVisible = source.hasLoginUrl
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_edit -> callBack.editSource(source.bookSourceUrl)
                R.id.menu_top -> callBack.toTop(source)
                R.id.menu_search -> callBack.searchBook(source)
                R.id.menu_login -> context.startActivity<SourceLoginActivity> {
                    putExtra("type", "bookSource")
                    putExtra("key", source.bookSourceUrl)
                }
                R.id.menu_refresh -> Coroutine.async(callBack.scope) {
                    source.clearExploreKindsCache()
                }.onSuccess {
                    notifyItemChanged(position)
                }
                R.id.menu_del -> callBack.deleteSource(source)
            }
            true
        }
        popupMenu.show()
        return true
    }

    interface CallBack {
        val scope: CoroutineScope
        fun scrollTo(pos: Int)
        fun openExplore(sourceUrl: String, title: String, exploreUrl: String?)
        fun editSource(sourceUrl: String)
        fun toTop(source: BookSourcePart)
        fun deleteSource(source: BookSourcePart)
        fun searchBook(bookSource: BookSourcePart)
    }
}
