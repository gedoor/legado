package io.legado.app.base

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import io.legado.app.App
import io.legado.app.R
import io.legado.app.constant.AppConst
import io.legado.app.constant.PreferKey
import io.legado.app.constant.Theme
import io.legado.app.help.AppConfig
import io.legado.app.help.ThemeConfig
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.widget.TitleBar
import io.legado.app.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel


abstract class BaseActivity<VB : ViewBinding>(
    val fullScreen: Boolean = true,
    private val theme: Theme = Theme.Auto,
    private val toolBarTheme: Theme = Theme.Auto,
    private val transparent: Boolean = false,
    private val imageBg: Boolean = true
) : AppCompatActivity(),
    CoroutineScope by MainScope() {

    protected val binding: VB by lazy { getViewBinding() }

    val isInMultiWindow: Boolean
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                isInMultiWindowMode
            } else {
                false
            }
        }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageUtils.setConfiguration(newBase))
    }

    protected abstract fun getViewBinding(): VB

    override fun onCreateView(
        parent: View?,
        name: String,
        context: Context,
        attrs: AttributeSet
    ): View? {
        if (AppConst.menuViewNames.contains(name) && parent?.parent is FrameLayout) {
            (parent.parent as View).setBackgroundColor(backgroundColor)
        }
        return super.onCreateView(parent, name, context, attrs)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            getPrefBoolean(PreferKey.highBrush)
        ) {
            /**
             * 添加高刷新率支持
             */
            // 获取系统window支持的模式
            @Suppress("DEPRECATION")
            val modes = window.windowManager.defaultDisplay.supportedModes
            // 对获取的模式，基于刷新率的大小进行排序，从小到大排序
            modes.sortBy {
                it.refreshRate
            }
            window.let {
                val lp = it.attributes
                // 取出最大的那一个刷新率，直接设置给window
                lp.preferredDisplayModeId = modes.last().modeId
                it.attributes = lp
            }
        }
        window.decorView.disableAutoFill()
        initTheme()
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupSystemBar()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            findViewById<TitleBar>(R.id.title_bar)
                ?.onMultiWindowModeChanged(isInMultiWindowMode, fullScreen)
        }
        onActivityCreated(savedInstanceState)
        observeLiveBus()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            App.navigationBarHeight = navigationBarHeight
        }
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration?) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        findViewById<TitleBar>(R.id.title_bar)
            ?.onMultiWindowModeChanged(isInMultiWindowMode, fullScreen)
        setupSystemBar()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        findViewById<TitleBar>(R.id.title_bar)
            ?.onMultiWindowModeChanged(isInMultiWindow, fullScreen)
        setupSystemBar()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    abstract fun onActivityCreated(savedInstanceState: Bundle?)

    final override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return menu?.let {
            val bool = onCompatCreateOptionsMenu(it)
            it.applyTint(this, toolBarTheme)
            bool
        } ?: super.onCreateOptionsMenu(menu)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        menu.applyOpenTint(this)
        return super.onMenuOpened(featureId, menu)
    }

    open fun onCompatCreateOptionsMenu(menu: Menu) = super.onCreateOptionsMenu(menu)

    final override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            supportFinishAfterTransition()
            return true
        }
        return onCompatOptionsItemSelected(item)
    }

    open fun onCompatOptionsItemSelected(item: MenuItem) = super.onOptionsItemSelected(item)

    private fun initTheme() {
        when (theme) {
            Theme.Transparent -> setTheme(R.style.AppTheme_Transparent)
            Theme.Dark -> {
                setTheme(R.style.AppTheme_Dark)
                ATH.applyBackgroundTint(window.decorView)
            }
            Theme.Light -> {
                setTheme(R.style.AppTheme_Light)
                ATH.applyBackgroundTint(window.decorView)
            }
            else -> {
                if (ColorUtils.isColorLight(primaryColor)) {
                    setTheme(R.style.AppTheme_Light)
                } else {
                    setTheme(R.style.AppTheme_Dark)
                }
                ATH.applyBackgroundTint(window.decorView)
            }
        }
        if (imageBg) {
            ThemeConfig.getBgImage(this)?.let {
                try {
                    window.decorView.background = it
                } catch (e: OutOfMemoryError) {
                    toastOnUi("Image Bg Out Of Memory")
                }
            }
        }
    }

    private fun setupSystemBar() {
        if (fullScreen && !isInMultiWindow) {
            ATH.fullScreen(this)
        }
        ATH.setStatusBarColorAuto(this, fullScreen)
        if (toolBarTheme == Theme.Dark) {
            ATH.setLightStatusBar(this, false)
        } else if (toolBarTheme == Theme.Light) {
            ATH.setLightStatusBar(this, true)
        }
        upNavigationBarColor()
    }

    open fun upNavigationBarColor() {
        if (AppConfig.immNavigationBar) {
            ATH.setNavigationBarColorAuto(this, ThemeStore.navigationBarColor(this))
        } else {
            val nbColor = ColorUtils.darkenColor(ThemeStore.navigationBarColor(this))
            ATH.setNavigationBarColorAuto(this, nbColor)
        }
    }

    open fun observeLiveBus() {
    }

    override fun finish() {
        currentFocus?.hideSoftInput()
        super.finish()
    }
}