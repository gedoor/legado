package io.legado.app.utils

import android.content.Context
import android.os.Build
import android.view.View
import android.view.View.*
import android.view.inputmethod.InputMethodManager
import io.legado.app.App

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