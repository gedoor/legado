package io.legado.app.utils

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import io.legado.app.App

fun View.hidehideSoftInput() = run {
    val imm = App.INSTANCE.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.let {
        imm.hideSoftInputFromWindow(this.windowToken, 0)
    }
}