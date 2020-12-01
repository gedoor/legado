package io.legado.app.ui.book.read

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.core.view.isVisible
import io.legado.app.App
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.databinding.ViewReadMenuBinding
import io.legado.app.help.AppConfig
import io.legado.app.help.LocalConfig
import io.legado.app.lib.theme.*
import io.legado.app.service.help.ReadBook
import io.legado.app.utils.*
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
    private val callBack: CallBack get() = activity as CallBack
    private val binding = ViewReadMenuBinding.inflate(LayoutInflater.from(context), this, true)
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
        if (AppConfig.isNightTheme) {
            binding.fabNightTheme.setImageResource(R.drawable.ic_daytime)
        } else {
            binding.fabNightTheme.setImageResource(R.drawable.ic_brightness)
        }
        initAnimation()
        val brightnessBackground = GradientDrawable()
        brightnessBackground.cornerRadius = 5F.dp
        brightnessBackground.setColor(ColorUtils.adjustAlpha(bgColor, 0.5f))
        binding.llBrightness.background = brightnessBackground
        binding.llBottomBg.setBackgroundColor(bgColor)
        binding.fabSearch.backgroundTintList = bottomBackgroundList
        binding.fabSearch.setColorFilter(textColor)
        binding.fabAutoPage.backgroundTintList = bottomBackgroundList
        binding.fabAutoPage.setColorFilter(textColor)
        binding.fabReplaceRule.backgroundTintList = bottomBackgroundList
        binding.fabReplaceRule.setColorFilter(textColor)
        binding.fabNightTheme.backgroundTintList = bottomBackgroundList
        binding.fabNightTheme.setColorFilter(textColor)
        binding.tvPre.setTextColor(textColor)
        binding.tvNext.setTextColor(textColor)
        binding.ivCatalog.setColorFilter(textColor)
        binding.tvCatalog.setTextColor(textColor)
        binding.ivReadAloud.setColorFilter(textColor)
        binding.tvReadAloud.setTextColor(textColor)
        binding.ivFont.setColorFilter(textColor)
        binding.tvFont.setTextColor(textColor)
        binding.ivSetting.setColorFilter(textColor)
        binding.tvSetting.setTextColor(textColor)
        binding.vwBg.onClick { }
        binding.vwNavigationBar.onClick { }
        binding.seekBrightness.progress = context.getPrefInt("brightness", 100)
        upBrightnessState()
        bindEvent()
    }

    fun upBrightnessState() {
        if (brightnessAuto()) {
            binding.ivBrightnessAuto.setColorFilter(context.accentColor)
            binding.seekBrightness.isEnabled = false
        } else {
            binding.ivBrightnessAuto.setColorFilter(context.buttonDisabledColor)
            binding.seekBrightness.isEnabled = true
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
        binding.titleBar.visible()
        binding.bottomMenu.visible()
        binding.titleBar.startAnimation(menuTopIn)
        binding.bottomMenu.startAnimation(menuBottomIn)
    }

    fun runMenuOut(onMenuOutEnd: (() -> Unit)? = null) {
        this.onMenuOutEnd = onMenuOutEnd
        if (this.isVisible) {
            binding.titleBar.startAnimation(menuTopOut)
            binding.bottomMenu.startAnimation(menuBottomOut)
        }
    }

    private fun brightnessAuto(): Boolean {
        return context.getPrefBoolean("brightnessAuto", true) || !showBrightnessView
    }

    private fun bindEvent() {
        binding.tvChapterName.onClick {
            callBack.openSourceEditActivity()
        }
        binding.tvChapterUrl.onClick {
            context.openUrl(binding.tvChapterUrl.text.toString())
        }
        binding.ivBrightnessAuto.onClick {
            context.putPrefBoolean("brightnessAuto", !brightnessAuto())
            upBrightnessState()
        }
        //亮度调节
        binding.seekBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                setScreenBrightness(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                context.putPrefInt("brightness", seekBar.progress)
            }

        })

        //阅读进度
        binding.seekBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                ReadBook.skipToPage(seekBar.progress)
            }
        })

        //搜索
        binding.fabSearch.onClick {
            runMenuOut {
                callBack.openSearchActivity(null)
            }
        }

        //自动翻页
        binding.fabAutoPage.onClick {
            runMenuOut {
                callBack.autoPage()
            }
        }

        //替换
        binding.fabReplaceRule.onClick { callBack.openReplaceRule() }

        //夜间模式
        binding.fabNightTheme.onClick {
            AppConfig.isNightTheme = !AppConfig.isNightTheme
            App.INSTANCE.applyDayNight()
        }

        //上一章
        binding.tvPre.onClick { ReadBook.moveToPrevChapter(upContent = true, toLast = false) }

        //下一章
        binding.tvNext.onClick { ReadBook.moveToNextChapter(true) }

        //目录
        binding.llCatalog.onClick {
            runMenuOut {
                callBack.openChapterList()
            }
        }

        //朗读
        binding.llReadAloud.onClick {
            runMenuOut {
                callBack.onClickReadAloud()
            }
        }
        binding.llReadAloud.onLongClick {
            runMenuOut { callBack.showReadAloudDialog() }
            true
        }
        //界面
        binding.llFont.onClick {
            runMenuOut {
                callBack.showReadStyle()
            }
        }

        //设置
        binding.llSetting.onClick {
            runMenuOut {
                callBack.showMoreSetting()
            }
        }
    }

    private fun initAnimation() {
        //显示菜单
        menuTopIn = AnimationUtilsSupport.loadAnimation(context, R.anim.anim_readbook_top_in)
        menuBottomIn = AnimationUtilsSupport.loadAnimation(context, R.anim.anim_readbook_bottom_in)
        menuTopIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                callBack.upSystemUiVisibility()
                binding.llBrightness.visible(showBrightnessView)
            }

            override fun onAnimationEnd(animation: Animation) {
                binding.vwMenuBg.onClick { runMenuOut() }
                binding.vwNavigationBar.layoutParams = binding.vwNavigationBar.layoutParams.apply {
                    height = activity!!.navigationBarHeight
                }
                if (LocalConfig.isFirstReadMenuShow) {
                    callBack.showReadMenuHelp()
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
                binding.vwMenuBg.setOnClickListener(null)
            }

            override fun onAnimationEnd(animation: Animation) {
                this@ReadMenu.invisible()
                binding.titleBar.invisible()
                binding.bottomMenu.invisible()
                cnaShowMenu = false
                onMenuOutEnd?.invoke()
                callBack.upSystemUiVisibility()
            }

            override fun onAnimationRepeat(animation: Animation) = Unit
        })
    }

    fun setTitle(title: String) {
        binding.titleBar.title = title
    }

    fun upBookView() {
        ReadBook.curTextChapter?.let {
            binding.tvChapterName.text = it.title
            binding.tvChapterName.visible()
            if (!ReadBook.isLocalBook) {
                binding.tvChapterUrl.text = it.url
                binding.tvChapterUrl.visible()
            } else {
                binding.tvChapterUrl.gone()
            }
            binding.seekReadPage.max = it.pageSize.minus(1)
            binding.seekReadPage.progress = ReadBook.durPageIndex
            binding.tvPre.isEnabled = ReadBook.durChapterIndex != 0
            binding.tvNext.isEnabled = ReadBook.durChapterIndex != ReadBook.chapterSize - 1
        } ?: let {
            binding.tvChapterName.gone()
            binding.tvChapterUrl.gone()
        }
    }

    fun setSeekPage(seek: Int) {
        binding.seekReadPage.progress = seek
    }

    fun setAutoPage(autoPage: Boolean) {
        if (autoPage) {
            binding.fabAutoPage.setImageResource(R.drawable.ic_auto_page_stop)
            binding.fabAutoPage.contentDescription = context.getString(R.string.auto_next_page_stop)
        } else {
            binding.fabAutoPage.setImageResource(R.drawable.ic_auto_page)
            binding.fabAutoPage.contentDescription = context.getString(R.string.auto_next_page)
        }
        binding.fabAutoPage.setColorFilter(textColor)
    }

    interface CallBack {
        fun autoPage()
        fun openReplaceRule()
        fun openChapterList()
        fun openSearchActivity(searchWord: String?)
        fun openSourceEditActivity()
        fun showReadStyle()
        fun showMoreSetting()
        fun showReadAloudDialog()
        fun upSystemUiVisibility()
        fun onClickReadAloud()
        fun showReadMenuHelp()
    }

}
