package io.legado.app.ui.book.read

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.WindowManager
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.core.view.isVisible
import io.legado.app.App
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.help.AppConfig
import io.legado.app.help.ReadBookConfig
import io.legado.app.lib.theme.*
import io.legado.app.service.help.ReadBook
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.view_read_menu.view.*
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.sdk27.listeners.onLongClick

/**
 * 阅读界面菜单
 */
class ReadMenu @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    var cnaShowMenu: Boolean = false
    private val callBack: CallBack? get() = activity as? CallBack
    private lateinit var menuTopIn: Animation
    private lateinit var menuTopOut: Animation
    private lateinit var menuBottomIn: Animation
    private lateinit var menuBottomOut: Animation
    private val bgColor: Int = context.bottomBackground
    private val textColor: Int = context.getPrimaryTextColor(ColorUtils.isColorLight(bgColor))
    private val bottomBackgroundList: ColorStateList = Selector.colorBuild()
        .setDefaultColor(bgColor)
        .setPressedColor(ColorUtils.darkenColor(bgColor))
        .create()
    private var onMenuOutEnd: (() -> Unit)? = null
    val showBrightnessView get() = context.getPrefBoolean(PreferKey.showBrightnessView, true)

    init {
        inflate(context, R.layout.view_read_menu, this)
        if (AppConfig.isNightTheme) {
            fabNightTheme.setImageResource(R.drawable.ic_daytime)
        } else {
            fabNightTheme.setImageResource(R.drawable.ic_brightness)
        }
        initAnimation()
        val brightnessBackground = GradientDrawable()
        brightnessBackground.cornerRadius = 5F.dp
        brightnessBackground.setColor(ColorUtils.adjustAlpha(bgColor, 0.5f))
        ll_brightness.background = brightnessBackground
        ll_bottom_bg.setBackgroundColor(bgColor)
        fabSearch.backgroundTintList = bottomBackgroundList
        fabSearch.setColorFilter(textColor)
        fabAutoPage.backgroundTintList = bottomBackgroundList
        fabAutoPage.setColorFilter(textColor)
        fabReplaceRule.backgroundTintList = bottomBackgroundList
        fabReplaceRule.setColorFilter(textColor)
        fabNightTheme.backgroundTintList = bottomBackgroundList
        fabNightTheme.setColorFilter(textColor)
        tv_pre.setTextColor(textColor)
        tv_next.setTextColor(textColor)
        iv_catalog.setColorFilter(textColor)
        tv_catalog.setTextColor(textColor)
        iv_read_aloud.setColorFilter(textColor)
        tv_read_aloud.setTextColor(textColor)
        iv_font.setColorFilter(textColor)
        tv_font.setTextColor(textColor)
        iv_setting.setColorFilter(textColor)
        tv_setting.setTextColor(textColor)
        vw_bg.onClick { }
        vwNavigationBar.onClick { }
        seek_brightness.progress = context.getPrefInt("brightness", 100)
        upBrightnessState()
        bindEvent()
    }

    fun upBrightnessState() {
        if (brightnessAuto()) {
            iv_brightness_auto.setColorFilter(context.accentColor)
            seek_brightness.isEnabled = false
        } else {
            iv_brightness_auto.setColorFilter(context.buttonDisabledColor)
            seek_brightness.isEnabled = true
        }
        setScreenBrightness(context.getPrefInt("brightness", 100))
    }

    /**
     * 设置屏幕亮度
     */
    private fun setScreenBrightness(value: Int) {
        var brightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        if (!brightnessAuto()) {
            brightness = value.toFloat()
            if (brightness < 1f) brightness = 1f
            brightness /= 255f
        }
        val params = activity?.window?.attributes
        params?.screenBrightness = brightness
        activity?.window?.attributes = params
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
            title_bar.startAnimation(menuTopOut)
            bottom_menu.startAnimation(menuBottomOut)
        }
    }

    private fun brightnessAuto(): Boolean {
        return context.getPrefBoolean("brightnessAuto", true) || !showBrightnessView
    }

    private fun bindEvent() {
        iv_brightness_auto.onClick {
            context.putPrefBoolean("brightnessAuto", !brightnessAuto())
            upBrightnessState()
        }
        //亮度调节
        seek_brightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                setScreenBrightness(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                context.putPrefInt("brightness", seek_brightness.progress)
            }

        })

        //阅读进度
        seek_read_page.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                ReadBook.skipToPage(seekBar.progress)
            }
        })

        //搜索
        fabSearch.onClick {
            runMenuOut {
                callBack?.openSearchActivity(null)
            }
        }

        //自动翻页
        fabAutoPage.onClick {
            runMenuOut {
                callBack?.autoPage()
            }
        }

        //替换
        fabReplaceRule.onClick { callBack?.openReplaceRule() }

        //夜间模式
        fabNightTheme.onClick {
            AppConfig.isNightTheme = !AppConfig.isNightTheme
            App.INSTANCE.applyDayNight()
        }

        //上一章
        tv_pre.onClick { ReadBook.moveToPrevChapter(upContent = true, toLast = false) }

        //下一章
        tv_next.onClick { ReadBook.moveToNextChapter(true) }

        //目录
        ll_catalog.onClick {
            runMenuOut {
                callBack?.openChapterList()
            }
        }

        //朗读
        ll_read_aloud.onClick {
            runMenuOut {
                callBack?.onClickReadAloud()
            }
        }
        ll_read_aloud.onLongClick {
            runMenuOut { callBack?.showReadAloudDialog() }
            true
        }
        //界面
        ll_font.onClick {
            runMenuOut {
                callBack?.showReadStyle()
            }
        }

        //设置
        ll_setting.onClick {
            runMenuOut {
                callBack?.showMoreSetting()
            }
        }
    }

    private fun initAnimation() {
        //显示菜单
        menuTopIn = AnimationUtilsSupport.loadAnimation(context, R.anim.anim_readbook_top_in)
        menuBottomIn = AnimationUtilsSupport.loadAnimation(context, R.anim.anim_readbook_bottom_in)
        menuTopIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                callBack?.upSystemUiVisibility()
                ll_brightness.visible(showBrightnessView)
            }

            override fun onAnimationEnd(animation: Animation) {
                vw_menu_bg.onClick { runMenuOut() }
                vwNavigationBar.layoutParams = vwNavigationBar.layoutParams.apply {
                    height =
                        if (ReadBookConfig.hideNavigationBar
                            && SystemUtils.isNavigationBarExist(activity)
                        )
                            context.navigationBarHeight
                        else 0
                }
            }

            override fun onAnimationRepeat(animation: Animation) = Unit
        })

        //隐藏菜单
        menuTopOut = AnimationUtilsSupport.loadAnimation(context, R.anim.anim_readbook_top_out)
        menuBottomOut =
            AnimationUtilsSupport.loadAnimation(context, R.anim.anim_readbook_bottom_out)
        menuTopOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                vw_menu_bg.setOnClickListener(null)
            }

            override fun onAnimationEnd(animation: Animation) {
                this@ReadMenu.invisible()
                title_bar.invisible()
                bottom_menu.invisible()
                cnaShowMenu = false
                onMenuOutEnd?.invoke()
                callBack?.upSystemUiVisibility()
            }

            override fun onAnimationRepeat(animation: Animation) = Unit
        })
    }

    fun setAutoPage(autoPage: Boolean) {
        if (autoPage) {
            fabAutoPage.setImageResource(R.drawable.ic_auto_page_stop)
            fabAutoPage.contentDescription = context.getString(R.string.auto_next_page_stop)
        } else {
            fabAutoPage.setImageResource(R.drawable.ic_auto_page)
            fabAutoPage.contentDescription = context.getString(R.string.auto_next_page)
        }
        fabAutoPage.setColorFilter(textColor)
    }

    interface CallBack {
        fun autoPage()
        fun openReplaceRule()
        fun openChapterList()
        fun openSearchActivity(searchWord: String?)
        fun showReadStyle()
        fun showMoreSetting()
        fun showReadAloudDialog()
        fun upSystemUiVisibility()
        fun onClickReadAloud()
    }

}
