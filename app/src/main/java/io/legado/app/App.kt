package io.legado.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import io.legado.app.constant.AppConst.channelIdDownload
import io.legado.app.constant.AppConst.channelIdReadAloud
import io.legado.app.constant.AppConst.channelIdWeb
import io.legado.app.data.AppDatabase
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefInt
import java.util.*

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
        initNightTheme()
        upThemeStore()
    }

    fun initNightTheme() {
        if (getPrefBoolean("isNightTheme", false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    fun upThemeStore() {
        if (getPrefBoolean("isNightTheme", false)) {
            ThemeStore.editTheme(this)
                .primaryColor(getPrefInt("colorPrimaryNight", resources.getColor(R.color.md_grey_800)))
                .accentColor(getPrefInt("colorAccentNight", resources.getColor(R.color.md_pink_800)))
                .backgroundColor(getPrefInt("colorBackgroundNight", resources.getColor(R.color.md_grey_800)))
                .apply()
        } else {
            ThemeStore.editTheme(this)
                .primaryColor(getPrefInt("colorPrimary", resources.getColor(R.color.md_grey_100)))
                .accentColor(getPrefInt("colorAccent", resources.getColor(R.color.md_pink_600)))
                .backgroundColor(getPrefInt("colorBackground", resources.getColor(R.color.md_grey_100)))
                .apply()
        }
    }

    /**
     * 创建通知ID
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannelId() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        //用唯一的ID创建渠道对象
        val downloadChannel = NotificationChannel(
            channelIdDownload,
            getString(R.string.download_offline),
            NotificationManager.IMPORTANCE_LOW
        )
        //初始化channel
        downloadChannel.enableLights(false)
        downloadChannel.enableVibration(false)
        downloadChannel.setSound(null, null)

        //用唯一的ID创建渠道对象
        val readAloudChannel = NotificationChannel(
            channelIdReadAloud,
            getString(R.string.read_aloud),
            NotificationManager.IMPORTANCE_LOW
        )
        //初始化channel
        readAloudChannel.enableLights(false)
        readAloudChannel.enableVibration(false)
        readAloudChannel.setSound(null, null)

        //用唯一的ID创建渠道对象
        val webChannel = NotificationChannel(
            channelIdWeb,
            getString(R.string.web_service),
            NotificationManager.IMPORTANCE_LOW
        )
        //初始化channel
        webChannel.enableLights(false)
        webChannel.enableVibration(false)
        webChannel.setSound(null, null)

        //向notification manager 提交channel
        notificationManager.createNotificationChannels(Arrays.asList(downloadChannel, readAloudChannel, webChannel))
    }
}
