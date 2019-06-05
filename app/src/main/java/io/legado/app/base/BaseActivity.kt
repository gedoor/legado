package io.legado.app.base

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import io.legado.app.R
import io.legado.app.lib.theme.ColorUtils
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.setIconColor


abstract class BaseActivity<VM : ViewModel> : AppCompatActivity() {

    protected abstract val viewModel: VM

    protected abstract val layoutID: Int

    override fun onCreate(savedInstanceState: Bundle?) {
        initTheme()
        setupSystemBar()
        super.onCreate(savedInstanceState)
        setContentView(layoutID)
        onViewModelCreated(viewModel, savedInstanceState)
    }

    abstract fun onViewModelCreated(viewModel: VM, savedInstanceState: Bundle?)

    final override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return menu?.let {
            val bool = onCompatCreateOptionsMenu(it)
            it.setIconColor(this)
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

    protected fun setupSystemBar() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        if (getPrefBoolean("transparentStatusBar", false)) {
            window.statusBarColor = Color.TRANSPARENT
        } else {
            window.statusBarColor = getCompatColor(R.color.status_bar_bag)
        }
    }
}