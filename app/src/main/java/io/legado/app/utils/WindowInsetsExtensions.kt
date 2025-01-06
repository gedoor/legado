package io.legado.app.utils

import androidx.core.view.WindowInsetsCompat

val WindowInsetsCompat.navigationBarHeight
    get() = (getInsets(WindowInsetsCompat.Type.systemBars()).bottom - imeHeight).coerceAtLeast(0)

val WindowInsetsCompat.imeHeight
    get() = getInsets(WindowInsetsCompat.Type.ime()).bottom
