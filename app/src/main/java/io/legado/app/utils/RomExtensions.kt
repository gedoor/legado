package io.legado.app.utils

import android.os.Build

val isVivoDevice by lazy {
    Build.MANUFACTURER.equals("vivo", ignoreCase = true)
}
