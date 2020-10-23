package io.legado.app.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Menu
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.PopupMenu
import io.legado.app.R
import io.legado.app.help.AppConfig
import io.legado.app.lib.theme.*
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.view_select_action_bar.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

@Suppress("unused")
class SelectActionBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    private var callBack: CallBack? = null
    private var selMenu: PopupMenu? = null

    init {
        setBackgroundColor(context.bottomBackground)
        elevation =
            if (AppConfig.elevation < 0) context.elevation else AppConfig.elevation.toFloat()
        View.inflate(context, R.layout.view_select_action_bar, this)
        val textIsDark = ColorUtils.isColorLight(context.bottomBackground)
        val primaryTextColor = context.getPrimaryTextColor(textIsDark)
        val secondaryTextColor = context.getSecondaryTextColor(textIsDark)
        cb_selected_all.setTextColor(primaryTextColor)
        TintHelper.setTint(cb_selected_all, context.accentColor, !textIsDark)
        iv_menu_more.setColorFilter(secondaryTextColor)
        cb_selected_all.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                callBack?.selectAll(isChecked)
            }
        }
        btn_revert_selection.onClick { callBack?.revertSelection() }
        btn_select_action_main.onClick { callBack?.onClickMainAction() }
        iv_menu_more.onClick { selMenu?.show() }
    }

    fun setMainActionText(text: String) {
        btn_select_action_main.text = text
        btn_select_action_main.visible()
    }

    fun setMainActionText(@StringRes id: Int) {
        btn_select_action_main.setText(id)
        btn_select_action_main.visible()
    }

    fun inflateMenu(@MenuRes resId: Int): Menu? {
        selMenu = PopupMenu(context, iv_menu_more)
        selMenu?.inflate(resId)
        iv_menu_more.visible()
        return selMenu?.menu
    }

    fun setCallBack(callBack: CallBack) {
        this.callBack = callBack
    }

    fun setOnMenuItemClickListener(listener: PopupMenu.OnMenuItemClickListener) {
        selMenu?.setOnMenuItemClickListener(listener)
    }

    fun upCountView(selectCount: Int, allCount: Int) {
        if (selectCount == 0) {
            cb_selected_all.isChecked = false
        } else {
            cb_selected_all.isChecked = selectCount >= allCount
        }

        //重置全选的文字
        if (cb_selected_all.isChecked) {
            cb_selected_all.text = context.getString(
                R.string.select_cancel_count,
                selectCount,
                allCount
            )
        } else {
            cb_selected_all.text = context.getString(
                R.string.select_all_count,
                selectCount,
                allCount
            )
        }
        setMenuClickable(selectCount > 0)
    }

    private fun setMenuClickable(isClickable: Boolean) {
        btn_revert_selection.isEnabled = isClickable
        btn_revert_selection.isClickable = isClickable
        btn_select_action_main.isEnabled = isClickable
        btn_select_action_main.isClickable = isClickable
    }

    interface CallBack {

        fun selectAll(selectAll: Boolean)

        fun revertSelection()

        fun onClickMainAction() {}
    }
}