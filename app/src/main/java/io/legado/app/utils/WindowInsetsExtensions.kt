package io.legado.app.utils

import androidx.core.view.WindowInsetsCompat

val WindowInsetsCompat.navigationBarHeight
    get() = getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

val WindowInsetsCompat.imeHeight
    get() = getInsets(WindowInsetsCompat.Type.ime()).bottom
