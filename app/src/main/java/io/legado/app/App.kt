package io.legado.app

import android.app.Application
import android.content.SharedPreferences

class App : Application() {

    companion object{
        lateinit var instance:App
        lateinit var configPreferences:SharedPreferences
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        configPreferences = getSharedPreferences("CONFIG", 0)

    }
}