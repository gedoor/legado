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
import io.legado.app.ui.widget.seekbar.SeekBarChangeListener
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
        initView()
        upBrightnessState()
        bindEvent()
    }

    private fun initView() = with(binding) {
        if (AppConfig.isNightTheme) {
            fabNightTheme.setImageResource(R.drawable.ic_daytime)
        } else {
            fabNightTheme.setImageResource(R.drawable.ic_brightness)
        }
        initAnimation()
        val brightnessBackground = GradientDrawable()
        brightnessBackground.cornerRadius = 5F.dp
        brightnessBackground.setColor(ColorUtils.adjustAlpha(bgColor, 0.5f))
        llBrightness.background = brightnessBackground
        llBottomBg.setBackgroundColor(bgColor)
        fabSearch.backgroundTintList = bottomBackgroundList
        fabSearch.setColorFilter(textColor)
        fabAutoPage.backgroundTintList = bottomBackgroundList
        fabAutoPage.setColorFilter(textColor)
        fabReplaceRule.backgroundTintList = bottomBackgroundList
        fabReplaceRule.setColorFilter(textColor)
        fabNightTheme.backgroundTintList = bottomBackgroundList
        fabNightTheme.setColorFilter(textColor)
        tvPre.setTextColor(textColor)
        tvNext.setTextColor(textColor)
        ivCatalog.setColorFilter(textColor)
        tvCatalog.setTextColor(textColor)
        ivReadAloud.setColorFilter(textColor)
        tvReadAloud.setTextColor(textColor)
        ivFont.setColorFilter(textColor)
        tvFont.setTextColor(textColor)
        ivSetting.setColorFilter(textColor)
        tvSetting.setTextColor(textColor)
        vwBg.onClick { }
        vwNavigationBar.onClick { }
        seekBrightness.progress = context.getPrefInt("brightness", 100)
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

    private fun bindEvent() = with(binding) {
        tvChapterName.onClick {
            callBack.openSourceEditActivity()
        }
        tvChapterUrl.onClick {
            context.openUrl(binding.tvChapterUrl.text.toString())
        }
        ivBrightnessAuto.onClick {
            context.putPrefBoolean("brightnessAuto", !brightnessAuto())
            upBrightnessState()
        }
        //亮度调节
        seekBrightness.setOnSeekBarChangeListener(object : SeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                setScreenBrightness(progress)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                context.putPrefInt("brightness", seekBar.progress)
            }

        })

        //阅读进度
        seekReadPage.setOnSeekBarChangeListener(object : SeekBarChangeListener {

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                ReadBook.skipToPage(seekBar.progress)
            }

        })

        //搜索
        fabSearch.onClick {
            runMenuOut {
                callBack.openSearchActivity(null)
            }
        }

        //自动翻页
        fabAutoPage.onClick {
            runMenuOut {
                callBack.autoPage()
            }
        }

        //替换
        fabReplaceRule.onClick { callBack.openReplaceRule() }

        //夜间模式
        fabNightTheme.onClick {
            AppConfig.isNightTheme = !AppConfig.isNightTheme
            App.INSTANCE.applyDayNight()
        }

        //上一章
        tvPre.onClick { ReadBook.moveToPrevChapter(upContent = true, toLast = false) }

        //下一章
        tvNext.onClick { ReadBook.moveToNextChapter(true) }

        //目录
        llCatalog.onClick {
            runMenuOut {
                callBack.openChapterList()
            }
        }

        //朗读
        llReadAloud.onClick {
            runMenuOut {
                callBack.onClickReadAloud()
            }
        }
        llReadAloud.onLongClick {
            runMenuOut { callBack.showReadAloudDialog() }
            true
        }
        //界面
        llFont.onClick {
            runMenuOut {
                callBack.showReadStyle()
            }
        }

        //设置
        llSetting.onClick {
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
                if (!LocalConfig.readMenuHelpVersionIsLast) {
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
            binding.seekReadPage.progress = ReadBook.durPageIndex()
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

    fun setAutoPage(autoPage: Boolean) = with(binding) {
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
        fun openSourceEditActivity()
        fun showReadStyle()
        fun showMoreSetting()
        fun showReadAloudDialog()
        fun upSystemUiVisibility()
        fun onClickReadAloud()
        fun showReadMenuHelp()
    }

}
