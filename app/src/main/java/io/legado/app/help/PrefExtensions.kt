package io.legado.app.help

import android.content.Context
import io.legado.app.constant.PreferKey
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefInt

val Context.isNightTheme: Boolean
    get() = getPrefBoolean("isNightTheme")

val Context.isTransparentStatusBar: Boolean
    get() = getPrefBoolean("transparentStatusBar", true)

val Context.isShowRSS: Boolean
    get() = getPrefBoolean(PreferKey.showRss, true)

val Context.threadCount: Int
    get() = getPrefInt(PreferKey.threadCount, 16)