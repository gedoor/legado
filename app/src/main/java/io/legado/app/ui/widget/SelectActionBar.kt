package io.legado.app.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.Menu
import android.widget.FrameLayout
import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.PopupMenu
import io.legado.app.R
import io.legado.app.databinding.ViewSelectActionBarBinding
import io.legado.app.help.AppConfig
import io.legado.app.lib.theme.*
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.visible
import org.jetbrains.anko.sdk27.listeners.onClick

@Suppress("unused")
class SelectActionBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    private var callBack: CallBack? = null
    private var selMenu: PopupMenu? = null
    private val binding =
        ViewSelectActionBarBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        setBackgroundColor(context.bottomBackground)
        elevation =
            if (AppConfig.elevation < 0) context.elevation else AppConfig.elevation.toFloat()
        val textIsDark = ColorUtils.isColorLight(context.bottomBackground)
        val primaryTextColor = context.getPrimaryTextColor(textIsDark)
        val secondaryTextColor = context.getSecondaryTextColor(textIsDark)
        binding.cbSelectedAll.setTextColor(primaryTextColor)
        TintHelper.setTint(binding.cbSelectedAll, context.accentColor, !textIsDark)
        binding.ivMenuMore.setColorFilter(secondaryTextColor)
        binding.cbSelectedAll.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                callBack?.selectAll(isChecked)
            }
        }
        binding.btnRevertSelection.onClick { callBack?.revertSelection() }
        binding.btnSelectActionMain.onClick { callBack?.onClickMainAction() }
        binding.ivMenuMore.onClick { selMenu?.show() }
    }

    fun setMainActionText(text: String) = with(binding) {
        btnSelectActionMain.text = text
        btnSelectActionMain.visible()
    }

    fun setMainActionText(@StringRes id: Int) = with(binding) {
        btnSelectActionMain.setText(id)
        btnSelectActionMain.visible()
    }

    fun inflateMenu(@MenuRes resId: Int): Menu? {
        selMenu = PopupMenu(context, binding.ivMenuMore)
        selMenu?.inflate(resId)
        binding.ivMenuMore.visible()
        return selMenu?.menu
    }

    fun setCallBack(callBack: CallBack) {
        this.callBack = callBack
    }

    fun setOnMenuItemClickListener(listener: PopupMenu.OnMenuItemClickListener) {
        selMenu?.setOnMenuItemClickListener(listener)
    }

    fun upCountView(selectCount: Int, allCount: Int) = with(binding) {
        if (selectCount == 0) {
            cbSelectedAll.isChecked = false
        } else {
            cbSelectedAll.isChecked = selectCount >= allCount
        }

        //重置全选的文字
        if (cbSelectedAll.isChecked) {
            cbSelectedAll.text = context.getString(
                R.string.select_cancel_count,
                selectCount,
                allCount
            )
        } else {
            cbSelectedAll.text = context.getString(
                R.string.select_all_count,
                selectCount,
                allCount
            )
        }
        setMenuClickable(selectCount > 0)
    }

    private fun setMenuClickable(isClickable: Boolean) = with(binding) {
        btnRevertSelection.isEnabled = isClickable
        btnRevertSelection.isClickable = isClickable
        btnSelectActionMain.isEnabled = isClickable
        btnSelectActionMain.isClickable = isClickable
    }

    interface CallBack {

        fun selectAll(selectAll: Boolean)

        fun revertSelection()

        fun onClickMainAction() {}
    }
}