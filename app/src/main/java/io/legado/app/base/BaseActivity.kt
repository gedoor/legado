package io.legado.app.base

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import io.legado.app.R
import io.legado.app.constant.AppConst
import io.legado.app.constant.Theme
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.lib.theme.primaryColor
import io.legado.app.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel


abstract class BaseActivity(
    private val layoutID: Int,
    private val fullScreen: Boolean = true,
    private val theme: Theme = Theme.Auto
) : AppCompatActivity(),
    CoroutineScope by MainScope() {

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
        window.decorView.disableAutoFill()
        initTheme()
        setupSystemBar()
        super.onCreate(savedInstanceState)
        setContentView(layoutID)
        onActivityCreated(savedInstanceState)
        observeLiveBus()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    abstract fun onActivityCreated(savedInstanceState: Bundle?)

    final override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return menu?.let {
            val bool = onCompatCreateOptionsMenu(it)
            it.applyTint(this, theme)
            bool
        } ?: super.onCreateOptionsMenu(menu)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu?): Boolean {
        menu?.let {
            menu.applyOpenTint(this)
            return super.onMenuOpened(featureId, menu)
        }
        return true
    }

    open fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        return super.onCreateOptionsMenu(menu)
    }

    final override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.let {
            if (it.itemId == android.R.id.home) {
                supportFinishAfterTransition()
                return true
            }
        }
        return item != null && onCompatOptionsItemSelected(item)
    }

    open fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }

    private fun initTheme() {
        ATH.applyBackgroundTint(window.decorView)
        when (theme) {
            Theme.Dark -> setTheme(R.style.AppTheme_Dark)
            Theme.Light -> setTheme(R.style.AppTheme_Light)
            else -> if (ColorUtils.isColorLight(primaryColor)) {
                setTheme(R.style.AppTheme_Light)
            } else {
                setTheme(R.style.AppTheme_Dark)
            }
        }
    }

    private fun setupSystemBar() {
        if (fullScreen) {
            window.clearFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                        or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
            )
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
        ATH.setStatusBarColorAuto(this, fullScreen)
        if (theme == Theme.Dark) {
            ATH.setLightStatusBar(this, false)
        } else if (theme == Theme.Light) {
            ATH.setLightStatusBar(this, true)
        }
        upNavigationBarColor()
    }

    open fun upNavigationBarColor() {
        ATH.setNavigationBarColorAuto(this)
    }

    open fun observeLiveBus() {
    }

    override fun finish() {
        currentFocus?.hideSoftInput()
        super.finish()
    }
}