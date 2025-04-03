package io.legado.app.constant

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.annotation.Keep
import cn.hutool.crypto.digest.DigestUtil
import io.legado.app.BuildConfig
import io.legado.app.help.update.AppVariant
import org.apache.commons.lang3.time.FastDateFormat
import splitties.init.appCtx

@Suppress("ConstPropertyName")
@SuppressLint("SimpleDateFormat")
object AppConst {

    const val APP_TAG = "Legado"

    const val channelIdDownload = "channel_download"
    const val channelIdReadAloud = "channel_read_aloud"
    const val channelIdWeb = "channel_web"

    const val UA_NAME = "User-Agent"

    const val MAX_THREAD = 9

    const val DEFAULT_WEBDAV_ID = -1L

    private const val OFFICIAL_SIGNATURE =
        "8DACBF25EC667C9B1374DB1450C1A866C2AAA1173016E80BF6AD2F06FABDDC08"
    private const val BETA_SIGNATURE =
        "93A28468B0F69E8D14C8A99AB45841CEF902BBBA3761BBFEE02E67CBA801563E"

    val timeFormat: FastDateFormat by lazy {
        FastDateFormat.getInstance("HH:mm")
    }

    val dateFormat: FastDateFormat by lazy {
        FastDateFormat.getInstance("yyyy/MM/dd HH:mm")
    }

    val fileNameFormat: FastDateFormat by lazy {
        FastDateFormat.getInstance("yy-MM-dd-HH-mm-ss")
    }

    const val imagePathKey = "imagePath"

    val menuViewNames = arrayOf(
        "com.android.internal.view.menu.ListMenuItemView",
        "androidx.appcompat.view.menu.ListMenuItemView"
    )

    @SuppressLint("PrivateResource")
    val sysElevation = appCtx.resources
        .getDimension(com.google.android.material.R.dimen.design_appbar_elevation)
        .toInt()

    val androidId: String by lazy {
        Settings.System.getString(appCtx.contentResolver, Settings.Secure.ANDROID_ID) ?: "null"
    }

    val appInfo: AppInfo by lazy {
        val appInfo = AppInfo()
        @Suppress("DEPRECATION")
        appCtx.packageManager.getPackageInfo(appCtx.packageName, PackageManager.GET_ACTIVITIES)
            ?.let {
                appInfo.versionName = it.versionName!!
                appInfo.appVariant = when {
                    it.packageName.contains("releaseA") -> AppVariant.BETA_RELEASEA
                    isBeta -> AppVariant.BETA_RELEASE
                    isOfficial -> AppVariant.OFFICIAL
                    else -> AppVariant.UNKNOWN
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    appInfo.versionCode = it.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    appInfo.versionCode = it.versionCode.toLong()
                }
            }
        appInfo
    }

    @Suppress("DEPRECATION")
    private val sha256Signature: String by lazy {
        val packageInfo =
            appCtx.packageManager.getPackageInfo(appCtx.packageName, PackageManager.GET_SIGNATURES)
        DigestUtil.sha256Hex(packageInfo.signatures!![0].toByteArray()).uppercase()
    }

    private val isOfficial = sha256Signature == OFFICIAL_SIGNATURE

    private val isBeta = sha256Signature == BETA_SIGNATURE || BuildConfig.DEBUG

    val charsets =
        arrayListOf("UTF-8", "GB2312", "GB18030", "GBK", "Unicode", "UTF-16", "UTF-16LE", "ASCII")

    @Keep
    data class AppInfo(
        var versionCode: Long = 0L,
        var versionName: String = "",
        var appVariant: AppVariant = AppVariant.UNKNOWN
    )

    /**
     * The authority of a FileProvider defined in a <provider> element in your app's manifest.
     */
    const val authority = BuildConfig.APPLICATION_ID + ".fileProvider"

}
