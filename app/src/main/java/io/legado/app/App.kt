package io.legado.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.multidex.MultiDexApplication
import com.jeremyliao.liveeventbus.LiveEventBus
import io.legado.app.constant.AppConst
import io.legado.app.constant.AppConst.channelIdDownload
import io.legado.app.constant.AppConst.channelIdReadAloud
import io.legado.app.constant.AppConst.channelIdWeb
import io.legado.app.help.ActivityHelp
import io.legado.app.help.AppConfig
import io.legado.app.help.CrashHandler
import io.legado.app.help.ThemeConfig.applyDayNight
import io.legado.app.help.http.HttpHelper
import io.legado.app.utils.LanguageUtils
import io.legado.app.utils.defaultSharedPreferences
import rxhttp.wrapper.param.RxHttp

class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        CrashHandler(this)
        LanguageUtils.setConfiguration(this)
        RxHttp.init(HttpHelper.client, BuildConfig.DEBUG)
        RxHttp.setOnParamAssembly {
            it.addHeader(AppConst.UA_NAME, AppConfig.userAgent)
        }
        createNotificationChannels()
        applyDayNight(this)
        LiveEventBus.config()
            .supportBroadcast(this)
            .lifecycleObserverAlwaysActive(true)
            .autoClear(false)
        registerActivityLifecycleCallbacks(ActivityHelp)
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(AppConfig)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        when (newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES,
            Configuration.UI_MODE_NIGHT_NO -> applyDayNight(this)
        }
    }

    /**
     * 创建通知ID
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.let {
            val downloadChannel = NotificationChannel(
                channelIdDownload,
                getString(R.string.action_download),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }

            val readAloudChannel = NotificationChannel(
                channelIdReadAloud,
                getString(R.string.read_aloud),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }

            val webChannel = NotificationChannel(
                channelIdWeb,
                getString(R.string.web_service),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }

            //向notification manager 提交channel
            it.createNotificationChannels(listOf(downloadChannel, readAloudChannel, webChannel))
        }
    }

    companion object {
        var navigationBarHeight = 0
    }

}
