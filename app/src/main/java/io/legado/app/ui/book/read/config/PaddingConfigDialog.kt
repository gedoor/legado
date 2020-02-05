package io.legado.app.ui.book.read.config

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.DialogFragment
import io.legado.app.R
import io.legado.app.constant.Bus
import io.legado.app.help.ReadBookConfig
import io.legado.app.ui.book.read.Help
import io.legado.app.utils.postEvent
import io.legado.app.utils.progressAdd
import kotlinx.android.synthetic.main.dialog_read_padding.*
import org.jetbrains.anko.sdk27.listeners.onClick

class PaddingConfigDialog : DialogFragment() {

    override fun onStart() {
        super.onStart()
        val dm = DisplayMetrics()
        activity?.let {
            Help.upSystemUiVisibility(it)
            it.windowManager?.defaultDisplay?.getMetrics(dm)
        }
        dialog?.window?.let {
            val attr = it.attributes
            attr.dimAmount = 0.0f
            it.attributes = attr
            it.setLayout((dm.widthPixels * 0.9).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_read_padding, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
        initView()
    }

    private fun initData() = with(ReadBookConfig.getConfig()) {
        seek_padding_top.progress = paddingTop
        seek_padding_bottom.progress = paddingBottom
        seek_padding_left.progress = paddingLeft
        seek_padding_right.progress = paddingRight
        tv_padding_top.text = paddingTop.toString()
        tv_padding_bottom.text = paddingBottom.toString()
        tv_padding_left.text = paddingLeft.toString()
        tv_padding_right.text = paddingRight.toString()
    }

    private fun initView() = with(ReadBookConfig.getConfig()) {
        iv_padding_top_add.onClick {
            seek_padding_top.progressAdd(1)
            postEvent(Bus.UP_CONFIG, true)
        }
        iv_padding_top_remove.onClick {
            seek_padding_top.progressAdd(-1)
            postEvent(Bus.UP_CONFIG, true)
        }
        iv_padding_bottom_add.onClick {
            seek_padding_bottom.progressAdd(1)
            postEvent(Bus.UP_CONFIG, true)
        }
        iv_padding_bottom_remove.onClick {
            seek_padding_bottom.progressAdd(-1)
            postEvent(Bus.UP_CONFIG, true)
        }
        iv_padding_left_add.onClick {
            seek_padding_left.progressAdd(1)
            postEvent(Bus.UP_CONFIG, true)
        }
        iv_padding_left_remove.onClick {
            seek_padding_left.progressAdd(-1)
            postEvent(Bus.UP_CONFIG, true)
        }
        iv_padding_right_add.onClick {
            seek_padding_right.progressAdd(1)
            postEvent(Bus.UP_CONFIG, true)
        }
        iv_padding_right_remove.onClick {
            seek_padding_right.progressAdd(-1)
            postEvent(Bus.UP_CONFIG, true)
        }

        seek_padding_top.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                paddingTop = progress
                tv_padding_top.text = paddingTop.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                postEvent(Bus.UP_CONFIG, true)
            }
        })
        seek_padding_bottom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                paddingBottom = progress
                tv_padding_bottom.text = paddingBottom.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                postEvent(Bus.UP_CONFIG, true)
            }
        })
        seek_padding_left.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                paddingLeft = progress
                tv_padding_left.text = paddingLeft.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                postEvent(Bus.UP_CONFIG, true)
            }
        })
        seek_padding_right.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                paddingRight = progress
                tv_padding_right.text = paddingRight.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                postEvent(Bus.UP_CONFIG, true)
            }
        })
    }

}
