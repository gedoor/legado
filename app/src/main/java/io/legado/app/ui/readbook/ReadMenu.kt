package io.legado.app.ui.readbook

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.SeekBar
import io.legado.app.R
import io.legado.app.utils.isNightTheme
import kotlinx.android.synthetic.main.view_read_menu.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class ReadMenu : FrameLayout {

    private var callback: Callback? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        inflate(context, R.layout.view_read_menu, this)
        if (context.isNightTheme) {
            fabNightTheme.setImageResource(R.drawable.ic_daytime)
        } else {
            fabNightTheme.setImageResource(R.drawable.ic_brightness)
        }
        vw_bg.onClick { }
        vwNavigationBar.onClick { }
    }

    fun setListener(callback: Callback) {
        this.callback = callback
        bindEvent()
    }

    private fun bindEvent() {
        ll_floating_button.onClick { callback?.dismiss() }

        //阅读进度
        seek_bar_read_page.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                callback?.skipToPage(seekBar.progress)
            }
        })

        //朗读
        fab_read_aloud.onClick { callback?.clickReadAloud() }

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
        ll_font.onClick { callback?.showReadStyle() }

        //设置
        ll_setting.onClick { callback?.showMoreSetting() }
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

    interface Callback {
        fun skipToPage(page: Int)

        fun clickReadAloud()

        fun autoPage()

        fun setNightTheme()

        fun skipPreChapter()

        fun skipNextChapter()

        fun openReplaceRule()

        fun openChapterList()

        fun openAdjust()

        fun showReadStyle()

        fun showMoreSetting()

        fun toast(id: Int)

        fun dismiss()
    }

}
