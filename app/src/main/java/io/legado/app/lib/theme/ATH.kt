package io.legado.app.lib.theme

import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.EdgeEffect
import android.widget.ScrollView
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.dp
import splitties.init.appCtx

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
object ATH {

    fun setEdgeEffectColor(view: RecyclerView?, @ColorInt color: Int) {
        view?.edgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
            override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
                val edgeEffect = super.createEdgeEffect(view, direction)
                edgeEffect.color = color
                return edgeEffect
            }
        }
    }

    fun setEdgeEffectColor(viewPager: ViewPager2?, @ColorInt color: Int) {
        try {
            val clazz = ViewPager2::class.java
            for (name in arrayOf("mLeftEdge", "mRightEdge")) {
                val field = clazz.getDeclaredField(name)
                field.isAccessible = true
                val edge = field.get(viewPager)
                (edge as EdgeEffect).color = color
            }
        } catch (ignored: Exception) {
        }
    }

    fun setEdgeEffectColor(scrollView: ScrollView?, @ColorInt color: Int) {
        try {
            val clazz = ScrollView::class.java
            for (name in arrayOf("mEdgeGlowTop", "mEdgeGlowBottom")) {
                val field = clazz.getDeclaredField(name)
                field.isAccessible = true
                val edge = field.get(scrollView)
                (edge as EdgeEffect).color = color
            }
        } catch (ignored: Exception) {
        }
    }

    //**************************************************************Directly*************************************************************//

    fun applyBottomNavigationColor(bottomBar: BottomNavigationView) {
        bottomBar.apply {
            val bgColor = context.bottomBackground
            setBackgroundColor(bgColor)
            val textIsDark = ColorUtils.isColorLight(bgColor)
            val textColor = context.getSecondaryTextColor(textIsDark)
            val colorStateList = Selector.colorBuild()
                .setDefaultColor(textColor)
                .setSelectedColor(ThemeStore.accentColor(context)).create()
            itemIconTintList = colorStateList
            itemTextColor = colorStateList
        }
    }

    fun applyEdgeEffectColor(view: View?) {
        when (view) {
            is RecyclerView -> view.edgeEffectFactory = DEFAULT_EFFECT_FACTORY
            is ViewPager2 -> setEdgeEffectColor(view, ThemeStore.primaryColor(view.context))
            is ScrollView -> setEdgeEffectColor(view, ThemeStore.primaryColor(view.context))
        }
    }

    fun getDialogBackground(): GradientDrawable {
        val background = GradientDrawable()
        background.cornerRadius = 3F.dp
        background.setColor(appCtx.backgroundColor)
        return background
    }

    private val DEFAULT_EFFECT_FACTORY = object : RecyclerView.EdgeEffectFactory() {
        override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
            val edgeEffect = super.createEdgeEffect(view, direction)
            edgeEffect.color = ThemeStore.primaryColor(view.context)
            return edgeEffect
        }
    }
}