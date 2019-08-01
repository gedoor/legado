package io.legado.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.view.PixelCopy
import android.view.View
import android.view.View.*
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import io.legado.app.App
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth


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
    val imm = App.INSTANCE.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.let {
        imm.hideSoftInputFromWindow(this.windowToken, 0)
    }
}

fun View.disableAutoFill() = run {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        this.importantForAutofill = IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
    }
}

fun View.gone() {
    visibility = GONE
}

fun View.invisible() {
    visibility = INVISIBLE
}

fun View.visible() {
    visibility = VISIBLE
}

fun View.screenshot(): Bitmap? {
    return runCatching {
        val screenshot = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val c = Canvas(screenshot)
        c.translate(-scrollX.toFloat(), -scrollY.toFloat())
        draw(c)
        screenshot
    }.getOrNull()
}