package io.legado.app

import android.app.Application
import io.legado.app.data.AppDatabase

class App : Application() {

    companion object {
        @JvmStatic
        lateinit var INSTANCE: App
            private set

        @JvmStatic
        lateinit var db: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        db = AppDatabase.createDatabase(INSTANCE)
    }
}
