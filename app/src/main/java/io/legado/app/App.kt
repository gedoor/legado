package io.legado.app

import android.app.Application

class App : Application() {

    companion object {
        @JvmStatic
        lateinit var INSTANCE: App
            private set
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
    }
}
