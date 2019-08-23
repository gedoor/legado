package io.legado.app.ui.readbook

import android.content.Context
import android.util.AttributeSet
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.core.view.isVisible
import io.legado.app.App
import io.legado.app.R
import io.legado.app.constant.Bus
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.view_read_menu.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class ReadMenu : FrameLayout {

    private var callback: Callback? = null
    var menuBarShow: Boolean = false
    private lateinit var menuTopIn: Animation
    private lateinit var menuTopOut: Animation
    private lateinit var menuBottomIn: Animation
    private lateinit var menuBottomOut: Animation
    private var onMenuOutEnd: (() -> Unit)? = null

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
        initAnimation()
        vw_bg.onClick { }
        vwNavigationBar.onClick { }
    }

    fun setListener(callback: Callback) {
        this.callback = callback
        bindEvent()
    }

    private fun initAnimation() {
        menuTopIn = AnimationUtils.loadAnimation(context, R.anim.anim_readbook_top_in)
        menuBottomIn = AnimationUtils.loadAnimation(context, R.anim.anim_readbook_bottom_in)
        menuBottomIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {

            }

            override fun onAnimationEnd(animation: Animation) {
                vw_menu_bg.onClick { runMenuOut() }
            }

            override fun onAnimationRepeat(animation: Animation) {

            }
        })

        //隐藏菜单
        menuTopOut = AnimationUtils.loadAnimation(context, R.anim.anim_readbook_top_out)
        menuBottomOut = AnimationUtils.loadAnimation(context, R.anim.anim_readbook_bottom_out)
        menuBottomOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                vw_menu_bg.setOnClickListener(null)
            }

            override fun onAnimationEnd(animation: Animation) {
                this@ReadMenu.invisible()
                title_bar.invisible()
                bottom_menu.invisible()
                menuBarShow = false
                onMenuOutEnd?.invoke()
            }

            override fun onAnimationRepeat(animation: Animation) {

            }
        })
    }

    fun runMenuIn() {
        this.visible()
        title_bar.visible()
        bottom_menu.visible()
        title_bar.startAnimation(menuTopIn)
        bottom_menu.startAnimation(menuBottomIn)
    }

    fun runMenuOut(onMenuOutEnd: (() -> Unit)? = null) {
        this.onMenuOutEnd = onMenuOutEnd
        if (this.isVisible) {
            if (bottom_menu.isVisible) {
                title_bar.startAnimation(menuTopOut)
                bottom_menu.startAnimation(menuBottomOut)
            }
        }
    }

    fun upReadProgress(max: Int, dur: Int) {
        seek_read_page.max = max
        seek_read_page.progress = dur
    }

    private fun bindEvent() {
        ll_floating_button.onClick { runMenuOut() }

        //阅读进度
        seek_read_page.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                callback?.skipToPage(seekBar.progress)
            }
        })

        //自动翻页
        fabAutoPage.onClick { callback?.autoPage() }

        //替换
        fabReplaceRule.onClick { callback?.openReplaceRule() }

        //夜间模式
        fabNightTheme.onClick {
            context.putPrefBoolean("isNightTheme", !context.isNightTheme)
            App.INSTANCE.applyDayNight()
        }

        //上一章
        tv_pre.onClick { callback?.skipPreChapter() }

        //下一章
        tv_next.onClick { callback?.skipNextChapter() }

        //目录
        ll_catalog.onClick {
            runMenuOut {
                callback?.openChapterList()
            }
        }

        //朗读
        ll_read_aloud.onClick {
            postEvent(Bus.READ_ALOUD, true)
            runMenuOut()
        }

        //界面
        ll_font.onClick {
            runMenuOut {
                callback?.showReadStyle()
            }
        }

        //设置
        ll_setting.onClick {
            runMenuOut {
                callback?.showMoreSetting()
            }
        }
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

        fun autoPage()

        fun skipToPage(page: Int)

        fun skipPreChapter()

        fun skipNextChapter()

        fun openReplaceRule()

        fun openChapterList()

        fun showReadStyle()

        fun showMoreSetting()
    }

}
