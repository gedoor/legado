package io.legado.app.base

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import io.legado.app.R
import io.legado.app.lib.theme.ColorUtils
import io.legado.app.lib.theme.DrawableUtils
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.lib.theme.getPrimaryTextColor

abstract class BaseActivity<VM : ViewModel> : AppCompatActivity() {

    protected abstract val viewModel: VM

    protected abstract val layoutID: Int

    override fun onCreate(savedInstanceState: Bundle?) {
        initTheme()
        super.onCreate(savedInstanceState)
        setContentView(layoutID)
        onViewModelCreated(viewModel, savedInstanceState)
    }

    open fun onViewModelCreated(viewModel: VM, savedInstanceState: Bundle?) {

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return menu?.let {
            if (it is MenuBuilder) {
                it.setOptionalIconsVisible(true)
            }
            val bool = onCompatCreateOptionsMenu(it)
            val primaryTextColor = getPrimaryTextColor(ColorUtils.isColorLight(ThemeStore.primaryColor(this)))
            val defaultTextColor = ContextCompat.getColor(this, R.color.tv_text_default)
            for (i in 0 until menu.size()) {
                (menu.getItem(i) as MenuItemImpl).let { item ->
                    //overflow：展开的item
                    DrawableUtils.setTint(
                        item.icon,
                        if (item.requiresOverflow()) defaultTextColor else primaryTextColor
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
            setTheme(R.style.AppTheme_Light)
        } else {
            setTheme(R.style.AppTheme_Dark)
        }
    }
}