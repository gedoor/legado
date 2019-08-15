package io.legado.app.ui.readbook

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.SeekBar
import io.legado.app.R
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.view_read_bottom_menu.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class ReadBottomMenu : FrameLayout {

    private var callback: Callback? = null

    val readProgress: SeekBar
        get() = hpb_read_progress

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private fun init(context: Context) {
        inflate(context, R.layout.view_read_bottom_menu, this)
        vw_bg.onClick { }
        vwNavigationBar.onClick { }
    }

    fun setNavigationBarHeight(height: Int) {
        vwNavigationBar.layoutParams.height = height
    }

    fun setListener(callback: Callback) {
        this.callback = callback
        bindEvent()
    }

    private fun bindEvent() {
        ll_read_aloud_timer.onClick { callback?.dismiss() }
        ll_floating_button.onClick { callback?.dismiss() }

        //阅读进度
        hpb_read_progress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                callback?.skipToPage(seekBar.progress)
            }
        })

        //朗读定时
        fab_read_aloud_timer.onClick { }

        //朗读
        fab_read_aloud.onClick { callback?.onMediaButton() }
        //长按停止朗读
        fab_read_aloud.onClick {
            true
        }

        //自动翻页
        fabAutoPage.onClick { callback?.autoPage() }

        //替换
        fabReplaceRule.onClick { callback?.openReplaceRule() }

        //夜间模式
        fabNightTheme.onClick { callback?.setNightTheme() }

        //上一章
        tv_pre.onClick { callback?.skipPreChapter() }

        //下一章
        tv_next.onClick { callback?.skipNextChapter() }

        //目录
        ll_catalog.onClick { callback?.openChapterList() }

        //调节
        ll_adjust.onClick { callback?.openAdjust() }

        //界面
        ll_font.onClick { callback?.openReadInterface() }

        //设置
        ll_setting.onClick { callback?.openMoreSetting() }

        tv_read_aloud_timer.onClick { }
    }

    fun setFabReadAloudImage(id: Int) {
        fab_read_aloud.setImageResource(id)
    }

    fun setReadAloudTimer(visibility: Boolean) {
        if (visibility) {
            ll_read_aloud_timer.visible()
        } else {
            ll_read_aloud_timer.gone()
        }
    }

    fun setReadAloudTimer(text: String) {
        tv_read_aloud_timer.text = text
    }

    fun setFabReadAloudText(text: String) {
        fab_read_aloud.contentDescription = text
    }

    fun setTvPre(enable: Boolean) {
        tv_pre.isEnabled = enable
    }

    fun setTvNext(enable: Boolean) {
        tv_next.isEnabled = enable
    }

    fun setAutoPage(autoPage: Boolean) {
        if (autoPage) {
            fabAutoPage.setImageResource(R.drawable.ic_auto_page_stop)
            fabAutoPage.contentDescription = context.getString(R.string.auto_next_page_stop)
        } else {
            fabAutoPage.setImageResource(R.drawable.ic_auto_page)
            fabAutoPage.contentDescription = context.getString(R.string.auto_next_page)
        }
    }

    fun setFabNightTheme(isNightTheme: Boolean) {
        if (isNightTheme) {
            fabNightTheme.setImageResource(R.drawable.ic_daytime)
        } else {
            fabNightTheme.setImageResource(R.drawable.ic_brightness)
        }
    }

    interface Callback {
        fun skipToPage(page: Int)

        fun onMediaButton()

        fun autoPage()

        fun setNightTheme()

        fun skipPreChapter()

        fun skipNextChapter()

        fun openReplaceRule()

        fun openChapterList()

        fun openAdjust()

        fun openReadInterface()

        fun openMoreSetting()

        fun toast(id: Int)

        fun dismiss()
    }

}
