package io.legado.app.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.SeekBar
import io.legado.app.R
import io.legado.app.lib.theme.bottomBackground
import io.legado.app.lib.theme.getPrimaryTextColor
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.progressAdd
import kotlinx.android.synthetic.main.view_detail_seek_bar.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class DetailSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs),
    SeekBar.OnSeekBarChangeListener {
    private val isBottomBackground: Boolean
    var valueFormat: ((progress: Int) -> String)? = null
    var onChanged: ((progress: Int) -> Unit)? = null
    var progress: Int
        get() = seek_bar.progress
        set(value) {
            seek_bar.progress = value
        }
    var max: Int
        get() = seek_bar.max
        set(value) {
            seek_bar.max = value
        }

    init {
        View.inflate(context, R.layout.view_detail_seek_bar, this)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.DetailSeekBar)
        isBottomBackground =
            typedArray.getBoolean(R.styleable.DetailSeekBar_isBottomBackground, false)
        tv_seek_title.text = typedArray.getText(R.styleable.DetailSeekBar_title)
        seek_bar.max = typedArray.getInteger(R.styleable.DetailSeekBar_max, 0)
        typedArray.recycle()
        if (isBottomBackground) {
            val isLight = ColorUtils.isColorLight(context.bottomBackground)
            val textColor = context.getPrimaryTextColor(isLight)
            tv_seek_title.setTextColor(textColor)
            iv_seek_plus.setColorFilter(textColor)
            iv_seek_reduce.setColorFilter(textColor)
            tv_seek_value.setTextColor(textColor)
        }
        iv_seek_plus.onClick {
            seek_bar.progressAdd(1)
            onChanged?.invoke(seek_bar.progress)
        }
        iv_seek_reduce.onClick {
            seek_bar.progressAdd(-1)
            onChanged?.invoke(seek_bar.progress)
        }
        seek_bar.setOnSeekBarChangeListener(this)
    }

    private fun upValue(progress: Int = seek_bar.progress) {
        valueFormat?.let {
            tv_seek_value.text = it.invoke(progress)
        } ?: let {
            tv_seek_value.text = progress.toString()
        }
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        upValue(progress)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {

    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        onChanged?.invoke(seek_bar.progress)
    }

}