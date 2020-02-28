package io.legado.app.ui.book.read

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.Menu
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.annotation.RequiresApi
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.size
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.utils.isAbsUrl
import io.legado.app.utils.sendToClip
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.item_fillet_text.view.*
import kotlinx.android.synthetic.main.popup_action_menu.view.*
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.share
import org.jetbrains.anko.toast

@SuppressLint("RestrictedApi")
class TextActionMenu(private val context: Context, private val callBack: CallBack) :
    PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT) {

    init {
        @SuppressLint("InflateParams")
        contentView = LayoutInflater.from(context).inflate(R.layout.popup_action_menu, null)

        isTouchable = true
        isOutsideTouchable = false
        isFocusable = false

        initRecyclerView()
    }

    private fun initRecyclerView() = with(contentView) {
        val adapter = Adapter(context)
        recycler_view.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        recycler_view.adapter = adapter
        val menu = MenuBuilder(context)
        SupportMenuInflater(context).inflate(R.menu.content_select_action, menu)
        adapter.setItems(menu.visibleItems)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val popupMenu = PopupMenu(context, iv_menu_more)
            onInitializeMenu(popupMenu.menu)
            if (popupMenu.menu.size > 0) {
                iv_menu_more.visible()
                popupMenu.setOnMenuItemClickListener { item ->
                    item.intent?.let {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            it.putExtra(Intent.EXTRA_PROCESS_TEXT, callBack.selectedText)
                            context.startActivity(it)
                        }
                    }
                    this@TextActionMenu.dismiss()
                    true
                }
            }
            iv_menu_more.onClick {
                popupMenu.show()
            }
        }
    }

    inner class Adapter(context: Context) :
        SimpleRecyclerAdapter<MenuItemImpl>(context, R.layout.item_text) {

        override fun convert(
            holder: ItemViewHolder,
            item: MenuItemImpl,
            payloads: MutableList<Any>
        ) {
            with(holder.itemView) {
                text_view.text = item.title
            }
        }

        override fun registerListener(holder: ItemViewHolder) {
            holder.itemView.onClick {
                getItem(holder.layoutPosition)?.let {
                    if (!callBack.onMenuItemSelected(it.itemId)) {
                        onMenuItemSelected(it)
                    }
                }
            }
        }
    }

    private fun onMenuItemSelected(item: MenuItemImpl) {
        when (item.itemId) {
            R.id.menu_copy -> context.sendToClip(callBack.selectedText)
            R.id.menu_share_str -> context.share(callBack.selectedText)
            R.id.menu_browser -> {
                try {
                    val intent = if (callBack.selectedText.isAbsUrl()) {
                        Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(callBack.selectedText)
                        }
                    } else {
                        Intent(Intent.ACTION_WEB_SEARCH).apply {
                            putExtra(SearchManager.QUERY, callBack.selectedText)
                        }
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                    context.toast(e.localizedMessage ?: "ERROR")
                }
            }
        }
        callBack.onMenuActionFinally()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun createProcessTextIntent(): Intent {
        return Intent()
            .setAction(Intent.ACTION_PROCESS_TEXT)
            .setType("text/plain")
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getSupportedActivities(): List<ResolveInfo> {
        return context.packageManager
            .queryIntentActivities(createProcessTextIntent(), 0)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun createProcessTextIntentForResolveInfo(info: ResolveInfo): Intent {
        return createProcessTextIntent()
            .putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false)
            .setClassName(info.activityInfo.packageName, info.activityInfo.name)
    }

    /**
     * Start with a menu Item order value that is high enough
     * so that your "PROCESS_TEXT" menu items appear after the
     * standard selection menu items like Cut, Copy, Paste.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun onInitializeMenu(menu: Menu) {
        // Start with a menu Item order value that is high enough
        // so that your "PROCESS_TEXT" menu items appear after the
        // standard selection menu items like Cut, Copy, Paste.
        var menuItemOrder = 100
        for (resolveInfo in getSupportedActivities()) {
            menu.add(
                Menu.NONE, Menu.NONE,
                menuItemOrder++, resolveInfo.loadLabel(context.packageManager)
            ).intent = createProcessTextIntentForResolveInfo(resolveInfo)
        }
    }

    interface CallBack {
        val selectedText: String

        fun onMenuItemSelected(itemId: Int): Boolean

        fun onMenuActionFinally()
    }
}