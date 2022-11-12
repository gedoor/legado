package io.legado.app.utils

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData

private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

fun <T> MutableLiveData<T>.sendValue(value: T) {
    mainHandler.post {
        this@sendValue.value = value
    }
}
