package io.legado.app.utils

import android.os.Environment

fun getSdPath() = Environment.getExternalStorageDirectory().absolutePath
