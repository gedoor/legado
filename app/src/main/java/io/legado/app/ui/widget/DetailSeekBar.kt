package io.legado.app.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.widget.LinearLayoutCompat
import io.legado.app.R
import io.legado.app.utils.progressAdd
import kotlinx.android.synthetic.main.view_detail_seek_bar.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class DetailSeekBar(context: Context, attrs: AttributeSet?) : LinearLayoutCompat(context, attrs),
    SeekBar.OnSeekBarChangeListener {

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
        gravity = Gravity.CENTER_VERTICAL
        View.inflate(context, R.layout.view_detail_seek_bar, this)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.DetailSeekBar)
        tv_seek_title.text = typedArray.getText(R.styleable.DetailSeekBar_title)
        seek_bar.max = typedArray.getInteger(R.styleable.DetailSeekBar_max, 0)
        typedArray.recycle()

        iv_seek_add.onClick {
            seek_bar.progressAdd(1)
            onChanged?.invoke(seek_bar.progress)
        }
        iv_seek_remove.onClick {
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