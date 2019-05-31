package io.legado.app.base

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
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

    /**
     * 设置MENU图标颜色
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val primaryTextColor = getPrimaryTextColor(ColorUtils.isColorLight(ThemeStore.primaryColor(this)))
        val defaultTextColor = ContextCompat.getColor(this, R.color.tv_text_default)
        for (i in 0 until menu.size()) {
            (menu.getItem(i) as MenuItemImpl).let {//overflow：展开的item
                DrawableUtils.setTint(it.icon, if (it.requiresOverflow()) defaultTextColor else primaryTextColor)
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

//    @SuppressLint("PrivateApi")
//    override fun onMenuOpened(featureId: Int, menu: Menu?): Boolean {
//        if (menu != null) {
//            //展开菜单显示图标
//            if (menu.javaClass.simpleName.equals("MenuBuilder", ignoreCase = true)) {
//                try {
//                    var method = menu.javaClass.getDeclaredMethod("setOptionalIconsVisible", java.lang.Boolean.TYPE)
//                    method.isAccessible = true
//                    method.invoke(menu, true)
//                    method = menu.javaClass.getDeclaredMethod("getNonActionItems")
//                    val menuItems = method.invoke(menu) as ArrayList<MenuItem>
//                    if (menuItems.isNotEmpty()) {
//                        for (menuItem in menuItems) {
//                            val drawable = menuItem.icon
//                            if (drawable != null) {
//                                drawable.mutate()
//                                drawable.setColorFilter(
//                                    resources.getColor(R.color.tv_text_default),
//                                    PorterDuff.Mode.SRC_ATOP
//                                )
//                            }
//                        }
//                    }
//                } catch (ignored: Exception) {
//                }
//            }
//        }
//        return super.onMenuOpened(featureId, menu)
//    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.let {
            if (it.itemId == android.R.id.home) {
                supportFinishAfterTransition()
                return true
            }
        }
        return item != null && onCompatOptionsItemSelected(item)
    }

    open fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        return true
    }

    protected fun initTheme() {
        if (ColorUtils.isColorLight(ThemeStore.primaryColor(this))) {
            setTheme(R.style.AppTheme_Light)
        } else {
            setTheme(R.style.AppTheme_Dark)
        }
    }
}