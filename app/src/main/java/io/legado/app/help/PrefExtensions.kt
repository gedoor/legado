package io.legado.app.help

import android.content.Context
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefInt


val Context.isShowRSS: Boolean
    get() = getPrefBoolean("showRss", true)

val Context.threadCount: Int
    get() = getPrefInt("threadCount", 6)