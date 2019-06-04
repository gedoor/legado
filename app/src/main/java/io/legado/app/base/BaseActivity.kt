package io.legado.app.base

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.core.view.forEach
import androidx.lifecycle.ViewModel
import io.legado.app.lib.theme.ColorUtils
import io.legado.app.lib.theme.DrawableUtils
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.lib.theme.getPrimaryTextColor
import io.legado.app.utils.getCompatColor


abstract class BaseActivity<VM : ViewModel> : AppCompatActivity() {

    protected abstract val viewModel: VM

    protected abstract val layoutID: Int

    override fun onCreate(savedInstanceState: Bundle?) {
        initTheme()
        super.onCreate(savedInstanceState)
        setupSystemBar()
        setContentView(layoutID)
        onViewModelCreated(viewModel, savedInstanceState)
    }

    abstract fun onViewModelCreated(viewModel: VM, savedInstanceState: Bundle?)

    final override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return menu?.let {
            if (it is MenuBuilder) {
                it.setOptionalIconsVisible(true)
            }
            val bool = onCompatCreateOptionsMenu(it)
            val primaryTextColor = getPrimaryTextColor(ColorUtils.isColorLight(ThemeStore.primaryColor(this)))
            val defaultTextColor = getCompatColor(io.legado.app.R.color.tv_text_default)
            menu.forEach { item ->
                (item as MenuItemImpl).let { impl ->
                    //overflow：展开的item
                    DrawableUtils.setTint(
                        impl.icon,
                        if (impl.requiresOverflow()) defaultTextColor else primaryTextColor
                    )
                }
            }
            bool
        } ?: super.onCreateOptionsMenu(menu)
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

    protected fun initTheme() {
        if (ColorUtils.isColorLight(ThemeStore.primaryColor(this))) {
            setTheme(io.legado.app.R.style.AppTheme_Light)
        } else {
            setTheme(io.legado.app.R.style.AppTheme_Dark)
        }
    }

    protected fun setupSystemBar() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
    }
}