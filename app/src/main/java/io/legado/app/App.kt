package io.legado.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import com.jeremyliao.liveeventbus.LiveEventBus
import io.legado.app.constant.AppConst.channelIdDownload
import io.legado.app.constant.AppConst.channelIdReadAloud
import io.legado.app.constant.AppConst.channelIdWeb
import io.legado.app.data.AppDatabase
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.utils.getCompatColor
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

    private var versionCode = 0

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        db = AppDatabase.createDatabase(INSTANCE)
        versionCode = try {
            packageManager.getPackageInfo(packageName, 0).versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            0
        }
        if (!ThemeStore.isConfigured(this, versionCode)) upThemeStore()
        initNightTheme()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createChannelId()
        LiveEventBus.get()
            .config()
            .supportBroadcast(this)
            .lifecycleObserverAlwaysActive(true)
            .autoClear(false)
    }

    fun initNightTheme() {
        if (getPrefBoolean("isNightTheme", false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    /**
     * 更新主题
     */
    fun upThemeStore() {
        if (getPrefBoolean("isNightTheme", false)) {
            ThemeStore.editTheme(this)
                .primaryColor(getPrefInt("colorPrimaryNight", getCompatColor(R.color.colorPrimary)))
                .accentColor(getPrefInt("colorAccentNight", getCompatColor(R.color.colorAccent)))
                .backgroundColor(getPrefInt("colorBackgroundNight", getCompatColor(R.color.md_grey_800)))
                .apply()
        } else {
            ThemeStore.editTheme(this)
                .primaryColor(getPrefInt("colorPrimary", getCompatColor(R.color.colorPrimary)))
                .accentColor(getPrefInt("colorAccent", getCompatColor(R.color.colorAccent)))
                .backgroundColor(getPrefInt("colorBackground", getCompatColor(R.color.md_grey_100)))
                .apply()
        }
    }

    fun applyDayNight() {
        upThemeStore()
        initNightTheme()
    }

    /**
     * 创建通知ID
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannelId() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.let {
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
            it.createNotificationChannels(Arrays.asList(downloadChannel, readAloudChannel, webChannel))
        }
    }
}
