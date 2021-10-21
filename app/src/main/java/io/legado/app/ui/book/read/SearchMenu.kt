package io.legado.app.ui.book.read

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.WindowManager
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.databinding.ViewSearchMenuBinding
import io.legado.app.help.*
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.*
import io.legado.app.model.ReadBook
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.ui.book.searchContent.SearchContentViewModel
import io.legado.app.ui.browser.WebViewActivity
import io.legado.app.ui.widget.seekbar.SeekBarChangeListener
import io.legado.app.utils.*
import splitties.views.*

/**
 * 阅读界面菜单
 */
class SearchMenu @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    private val searchView: SearchView by lazy {
        binding.titleBar.findViewById(R.id.search_view)
    }

    val viewModel by viewModels<SearchContentViewModel>()

    private val callBack: CallBack get() = activity as CallBack
    private val binding = ViewSearchMenuBinding.inflate(LayoutInflater.from(context), this, true)
    private val menuTopIn: Animation = AnimationUtilsSupport.loadAnimation(context, R.anim.anim_readbook_top_in)
    private val menuTopOut: Animation = AnimationUtilsSupport.loadAnimation(context, R.anim.anim_readbook_top_out)
    private val menuBottomIn: Animation = AnimationUtilsSupport.loadAnimation(context, R.anim.anim_readbook_bottom_in)
    private val menuBottomOut: Animation = AnimationUtilsSupport.loadAnimation(context, R.anim.anim_readbook_bottom_out)
    private val bgColor: Int = context.bottomBackground
    private val textColor: Int = context.getPrimaryTextColor(ColorUtils.isColorLight(bgColor))
    private val bottomBackgroundList: ColorStateList =
        Selector.colorBuild().setDefaultColor(bgColor).setPressedColor(ColorUtils.darkenColor(bgColor)).create()
    private var onMenuOutEnd: (() -> Unit)? = null
    private var hasSearchResult: Boolean  = true

    init {
        initAnimation()
        initView()
        bindEvent()
    }

    private fun initView() = binding.run {
        llSearchBaseInfo.setBackgroundColor(bgColor)
        tvCurrentSearchInfo.setTextColor(bottomBackgroundList)
        llBottomBg.setBackgroundColor(bgColor)
        fabLeft.backgroundTintList = bottomBackgroundList
        fabLeft.setColorFilter(textColor)
        fabRight.backgroundTintList = bottomBackgroundList
        fabRight.setColorFilter(textColor)
        tvMainMenu.setTextColor(textColor)
        tvSearchResults.setTextColor(textColor)
        tvSearchExit.setTextColor(textColor)
        tvSetting.setTextColor(textColor)
        ivMainMenu.setColorFilter(textColor)
        ivSearchResults.setColorFilter(textColor)
        ivSearchExit.setColorFilter(textColor)
        ivSetting.setColorFilter(textColor)
        ivSearchContentBottom.setColorFilter(textColor)
        ivSearchContentTop.setColorFilter(textColor)
    }


    fun runMenuIn() {
        this.visible()
        binding.titleBar.visible()
        binding.llSearchBaseInfo.visible()
        binding.llBottomBg.visible()
        binding.titleBar.startAnimation(menuTopIn)
        binding.llSearchBaseInfo.startAnimation(menuBottomIn)
        binding.llBottomBg.startAnimation(menuBottomIn)
    }

    fun runMenuOut(onMenuOutEnd: (() -> Unit)? = null) {
        this.onMenuOutEnd = onMenuOutEnd
        if (this.isVisible) {
            binding.titleBar.startAnimation(menuTopOut)
            binding.llSearchBaseInfo.startAnimation(menuBottomOut)
            binding.llBottomBg.startAnimation(menuBottomOut)
        }
    }

    private fun bindEvent() = binding.run {
        titleBar.toolbar.setOnClickListener {
            ReadBook.book?.let {
                context.startActivity<BookInfoActivity> {
                    putExtra("name", it.name)
                    putExtra("author", it.author)
                }
            }
        }

        llSearchResults.setOnClickListener {
            runMenuOut {
                callBack.returnSearchActivity()
            }
        }

        //主菜单
        llMainMenu.setOnClickListener {
            runMenuOut {
                callBack.showMenuBar()
            }
        }


        //目录
        llSearchExit.setOnClickListener {
            runMenuOut {
                callBack.searchExit()
            }
        }

        //设置
        llSetting.setOnClickListener {
            runMenuOut {
                callBack.showSearchSetting()
            }
        }
    }

    private fun initAnimation() {
        //显示菜单
        menuTopIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                callBack.upSystemUiVisibility()
                binding.fabLeft.visible(hasSearchResult)
                binding.fabRight.visible(hasSearchResult)
            }

            @SuppressLint("RtlHardcoded")
            override fun onAnimationEnd(animation: Animation) {
                val navigationBarHeight = if (ReadBookConfig.hideNavigationBar) {
                    activity?.navigationBarHeight ?: 0
                } else {
                    0
                }
                binding.run {
                    vwMenuBg.setOnClickListener { runMenuOut() }
                    root.padding = 0
                    when (activity?.navigationBarGravity) {
                        Gravity.BOTTOM -> root.bottomPadding = navigationBarHeight
                        Gravity.LEFT   -> root.leftPadding = navigationBarHeight
                        Gravity.RIGHT  -> root.rightPadding = navigationBarHeight
                    }
                }
                callBack.upSystemUiVisibility()
            }

            override fun onAnimationRepeat(animation: Animation) = Unit
        })

        //隐藏菜单
        menuTopOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                binding.vwMenuBg.setOnClickListener(null)
            }

            override fun onAnimationEnd(animation: Animation) {
                this@SearchMenu.invisible()
                binding.titleBar.invisible()
                binding.llSearchBaseInfo.invisible()
                binding.llBottomBg.invisible()
                binding.fabRight.invisible()
                binding.fabLeft.invisible()
                onMenuOutEnd?.invoke()
                callBack.upSystemUiVisibility()
            }

            override fun onAnimationRepeat(animation: Animation) = Unit
        })
    }

    interface CallBack {
        var isShowingSearchResult: Boolean
        fun returnSearchActivity()
        fun showSearchSetting()
        fun upSystemUiVisibility()
        fun searchExit()
        fun showMenuBar()
    }

}
