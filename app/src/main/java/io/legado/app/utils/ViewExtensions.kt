@file:Suppress("unused")

package io.legado.app.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.text.Html
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.get
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.theme.TintHelper
import splitties.systemservices.inputMethodManager

import java.lang.reflect.Field


private tailrec fun getCompatActivity(context: Context?): AppCompatActivity? {
    return when (context) {
        is AppCompatActivity -> context
        is androidx.appcompat.view.ContextThemeWrapper -> getCompatActivity(context.baseContext)
        is android.view.ContextThemeWrapper -> getCompatActivity(context.baseContext)
        else -> null
    }
}

val View.activity: AppCompatActivity?
    get() = getCompatActivity(context)

fun View.hideSoftInput() = run {
    inputMethodManager.hideSoftInputFromWindow(this.windowToken, 0)
}

fun EditText.showSoftInput() = run {
    requestFocus()
    inputMethodManager.showSoftInput(this, InputMethodManager.RESULT_SHOWN)
}

fun View.disableAutoFill() = run {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        this.importantForAutofill = IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
    }
}

fun View.applyTint(
    @ColorInt color: Int,
    isDark: Boolean = AppConfig.isNightTheme(context)
) {
    TintHelper.setTintAuto(this, color, false, isDark)
}

fun View.applyBackgroundTint(
    @ColorInt color: Int,
    isDark: Boolean = AppConfig.isNightTheme
) {
    if (background == null) {
        setBackgroundColor(color)
    } else {
        TintHelper.setTintAuto(this, color, true, isDark)
    }
}

fun RecyclerView.setEdgeEffectColor(@ColorInt color: Int) {
    edgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
        override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
            val edgeEffect = super.createEdgeEffect(view, direction)
            edgeEffect.color = color
            return edgeEffect
        }
    }
}

fun ViewPager.setEdgeEffectColor(@ColorInt color: Int) {
    try {
        val clazz = ViewPager::class.java
        for (name in arrayOf("mLeftEdge", "mRightEdge")) {
            val field = clazz.getDeclaredField(name)
            field.isAccessible = true
            val edge = field.get(this)
            (edge as EdgeEffect).color = color
        }
    } catch (ignored: Exception) {
    }
}

fun EditText.disableEdit() {
    keyListener = null
}

fun View.gone() {
    if (visibility != GONE) {
        visibility = GONE
    }
}

fun View.gone(gone: Boolean) {
    if (gone) {
        gone()
    } else {
        visibility = VISIBLE
    }
}

fun View.invisible() {
    if (visibility != INVISIBLE) {
        visibility = INVISIBLE
    }
}

fun View.visible() {
    if (visibility != VISIBLE) {
        visibility = VISIBLE
    }
}

fun View.visible(visible: Boolean) {
    if (visible && visibility != VISIBLE) {
        visibility = VISIBLE
    } else if (!visible && visibility == VISIBLE) {
        visibility = INVISIBLE
    }
}

fun View.screenshot(): Bitmap? {
    return if (width > 0 && height > 0) {
        val screenshot = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(screenshot)
        c.translate(-scrollX.toFloat(), -scrollY.toFloat())
        this.draw(c)
        c.setBitmap(null)
        screenshot.prepareToDraw()
        screenshot
    } else {
        null
    }
}

fun View.setPaddingBottom(bottom: Int) {
    setPadding(paddingLeft, paddingTop, paddingRight, bottom)
}

fun SeekBar.progressAdd(int: Int) {
    progress += int
}

fun RadioGroup.getIndexById(id: Int): Int {
    for (i in 0 until this.childCount) {
        if (id == get(i).id) {
            return i
        }
    }
    return 0
}

fun RadioGroup.getCheckedIndex(): Int {
    for (i in 0 until this.childCount) {
        if (checkedRadioButtonId == get(i).id) {
            return i
        }
    }
    return 0
}

fun RadioGroup.checkByIndex(index: Int) {
    check(get(index).id)
}

@SuppressLint("ObsoleteSdkInt")
fun TextView.setHtml(html: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        text = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
    } else {
        @Suppress("DEPRECATION")
        text = Html.fromHtml(html)
    }
}

@SuppressLint("RestrictedApi")
fun PopupMenu.show(x: Int, y: Int) {
    kotlin.runCatching {
        val field: Field = this.javaClass.getDeclaredField("mPopup")
        field.isAccessible = true
        (field.get(this) as MenuPopupHelper).show(x, y)
    }.onFailure {
        it.printOnDebug()
    }
}

fun View.shouldHideSoftInput(event: MotionEvent): Boolean {
    if (this is EditText) {
        val l = intArrayOf(0, 0)
        getLocationInWindow(l)
        val left = l[0]
        val top = l[1]
        val bottom = top + getHeight()
        val right = left + getWidth()
        return !(event.x > left && event.x < right && event.y > top && event.y < bottom)
    }
    return false
}
