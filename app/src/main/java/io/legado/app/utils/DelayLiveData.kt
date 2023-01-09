package io.legado.app.utils

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData

class DelayLiveData<T>(val delay: Int) : LiveData<T>() {
    private val handler = Handler(Looper.getMainLooper())
    private val sendRunnable = Runnable { sendData() }
    private var postTime = 0L
    private var data: T? = null

    private fun sendData() {
        data?.let {
            super.postValue(it)
        }
    }

    public override fun postValue(value: T) {
        data = value
        if (System.currentTimeMillis() >= postTime + delay) {
            handler.removeCallbacks(sendRunnable)
            postTime = System.currentTimeMillis()
            super.postValue(value)
        } else {
            handler.removeCallbacks(sendRunnable)
            handler.postDelayed(sendRunnable, delay - System.currentTimeMillis() + postTime)
        }
    }
}