package io.legado.app.ui.book.read

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.animation.Animation
import android.widget.FrameLayout
import androidx.core.view.isVisible
import io.legado.app.R
import io.legado.app.constant.EventBus
import io.legado.app.databinding.ViewSearchMenuBinding
import io.legado.app.help.*
import io.legado.app.lib.theme.*
import io.legado.app.model.ReadBook
import io.legado.app.ui.book.searchContent.SearchResult
import io.legado.app.utils.*
import splitties.views.*

/**
 * 搜索界面菜单
 */
class SearchMenu @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val callBack: CallBack get() = activity as CallBack
    private val binding = ViewSearchMenuBinding.inflate(LayoutInflater.from(context), this, true)

    private val menuBottomIn: Animation = AnimationUtilsSupport.loadAnimation(context, R.anim.anim_readbook_bottom_in)
    private val menuBottomOut: Animation = AnimationUtilsSupport.loadAnimation(context, R.anim.anim_readbook_bottom_out)
    private val bgColor: Int = context.bottomBackground
    private val textColor: Int = context.getPrimaryTextColor(ColorUtils.isColorLight(bgColor))
    private val bottomBackgroundList: ColorStateList =
        Selector.colorBuild().setDefaultColor(bgColor).setPressedColor(ColorUtils.darkenColor(bgColor)).create()
    private var onMenuOutEnd: (() -> Unit)? = null

    private val searchResultList: MutableList<SearchResult> = mutableListOf()
    private var currentSearchResultIndex: Int = 0
    private var lastSearchResultIndex: Int = 0
    private val hasSearchResult: Boolean
        get() = searchResultList.isNotEmpty()
    val selectedSearchResult: SearchResult?
        get() = if (searchResultList.isNotEmpty()) searchResultList[currentSearchResultIndex] else null
    val previousSearchResult: SearchResult
        get() = searchResultList[lastSearchResultIndex]

    init {
        initAnimation()
        initView()
        bindEvent()
        updateSearchInfo()
        observeSearchResultList()
    }

    private fun observeSearchResultList() {
        activity?.let { owner ->
            eventObservable<List<SearchResult>>(EventBus.SEARCH_RESULT).observe(owner, {
                searchResultList.clear()
                searchResultList.addAll(it)
                updateSearchInfo()
            })
        }
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
        //tvSetting.setTextColor(textColor)
        ivMainMenu.setColorFilter(textColor)
        ivSearchResults.setColorFilter(textColor)
        ivSearchExit.setColorFilter(textColor)
        //ivSetting.setColorFilter(textColor)
        ivSearchContentUp.setColorFilter(textColor)
        ivSearchContentDown.setColorFilter(textColor)
        tvCurrentSearchInfo.setTextColor(textColor)
    }


    fun runMenuIn() {
        this.visible()
        binding.llSearchBaseInfo.visible()
        binding.llBottomBg.visible()
        binding.vwMenuBg.visible()
        binding.llSearchBaseInfo.startAnimation(menuBottomIn)
        binding.llBottomBg.startAnimation(menuBottomIn)
    }

    fun runMenuOut(onMenuOutEnd: (() -> Unit)? = null) {
        this.onMenuOutEnd = onMenuOutEnd
        if (this.isVisible) {
            binding.llSearchBaseInfo.startAnimation(menuBottomOut)
            binding.llBottomBg.startAnimation(menuBottomOut)
        }
    }

    fun updateSearchInfo() {
        ReadBook.curTextChapter?.let {
            binding.tvCurrentSearchInfo.text = context.getString(R.string.search_content_size) + ": ${searchResultList.size} / 当前章节: ${it.title}"
        }
    }

    fun updateSearchResultIndex(updateIndex: Int) {
        lastSearchResultIndex = currentSearchResultIndex
        currentSearchResultIndex = when {
            updateIndex < 0                      -> 0
            updateIndex >= searchResultList.size -> searchResultList.size - 1
            else                                 -> updateIndex
        }
    }

    private fun bindEvent() = binding.run {

        llSearchResults.setOnClickListener {
            runMenuOut {
                callBack.openSearchActivity(selectedSearchResult?.query)
            }
        }

        //主菜单
        llMainMenu.setOnClickListener {
            runMenuOut {
                callBack.showMenuBar()
                this@SearchMenu.invisible()
            }
        }

        //目录
        llSearchExit.setOnClickListener {
            runMenuOut {
                callBack.exitSearchMenu()
                this@SearchMenu.invisible()
            }
        }

        //设置
//        llSetting.setOnClickListener {
//            runMenuOut {
//                callBack.showSearchSetting()
//            }
//        }

        fabLeft.setOnClickListener {
            updateSearchResultIndex(currentSearchResultIndex - 1)
            callBack.navigateToSearch(searchResultList[currentSearchResultIndex])
        }

        ivSearchContentUp.setOnClickListener {
            updateSearchResultIndex(currentSearchResultIndex - 1)
            callBack.navigateToSearch(searchResultList[currentSearchResultIndex])
        }

        ivSearchContentDown.setOnClickListener {
            updateSearchResultIndex(currentSearchResultIndex + 1)
            callBack.navigateToSearch(searchResultList[currentSearchResultIndex])
        }

        fabRight.setOnClickListener {
            updateSearchResultIndex(currentSearchResultIndex + 1)
            callBack.navigateToSearch(searchResultList[currentSearchResultIndex])
        }
    }

    private fun initAnimation() {
        //显示菜单
        menuBottomIn.setAnimationListener(object : Animation.AnimationListener {
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
        menuBottomOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                binding.vwMenuBg.setOnClickListener(null)
            }

            override fun onAnimationEnd(animation: Animation) {
                binding.llSearchBaseInfo.invisible()
                binding.llBottomBg.invisible()
                binding.vwMenuBg.invisible()
                binding.vwMenuBg.setOnClickListener { runMenuOut() }

                onMenuOutEnd?.invoke()
                callBack.upSystemUiVisibility()
            }

            override fun onAnimationRepeat(animation: Animation) = Unit
        })
    }

    interface CallBack {
        var isShowingSearchResult: Boolean
        fun openSearchActivity(searchWord: String?)
        fun showSearchSetting()
        fun upSystemUiVisibility()
        fun exitSearchMenu()
        fun showMenuBar()
        fun navigateToSearch(searchResult: SearchResult)
    }

}
