package io.legado.app.lib.permission

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.constant.AppLog
import io.legado.app.exception.NoStackTraceException
import io.legado.app.utils.registerForActivityResult
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.launch

class PermissionActivity : AppCompatActivity() {

    private var rationaleDialog: AlertDialog? = null

    private val settingActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            onRequestPermissionFinish()
        }
    private val settingActivityResultAwait =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    private val requestPermissionResult =
        registerForActivityResult(ActivityResultContracts.RequestPermission())
    private val requestPermissionsResult =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())

    @SuppressLint("BatteryLife")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rationale = intent.getStringExtra(KEY_RATIONALE)
        val requestCode = intent.getIntExtra(KEY_INPUT_PERMISSIONS_CODE, 1000)
        val permissions = intent.getStringArrayExtra(KEY_INPUT_PERMISSIONS)!!
        when (intent.getIntExtra(KEY_INPUT_REQUEST_TYPE, Request.TYPE_REQUEST_PERMISSION)) {
            //权限请求
            Request.TYPE_REQUEST_PERMISSION -> showSettingDialog(permissions, rationale) {
                lifecycleScope.launch {
                    try {
                        val result = requestPermissionsResult.launch(permissions)
                        if (result.values.all { it }) {
                            onRequestPermissionFinish()
                        } else {
                            openSettingsActivity()
                        }
                    } catch (e: Exception) {
                        AppLog.put("请求权限出错\n$e", e, true)
                        RequestPlugins.sRequestCallback?.onError(e)
                        finish()
                    }
                }
            }
            //跳转到设置界面
            Request.TYPE_REQUEST_SETTING -> showSettingDialog(permissions, rationale) {
                openSettingsActivity()
            }
            //所有文件的管理权限
            Request.TYPE_MANAGE_ALL_FILES_ACCESS -> showSettingDialog(permissions, rationale) {
                try {
                    if (Permissions.isManageExternalStorage()) {
                        val settingIntent =
                            Intent(
                                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                        settingActivityResult.launch(settingIntent)
                    } else {
                        throw NoStackTraceException("no MANAGE_ALL_FILES_ACCESS_PERMISSION")
                    }
                } catch (e: Exception) {
                    AppLog.put("请求所有文件的管理权限出错\n$e", e, true)
                    RequestPlugins.sRequestCallback?.onError(e)
                    finish()
                }
            }

            Request.TYPE_REQUEST_NOTIFICATIONS -> showSettingDialog(permissions, rationale) {
                lifecycleScope.launch {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                            && requestPermissionResult.launch(Permissions.POST_NOTIFICATIONS)
                        ) {
                            onRequestPermissionFinish()
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            //这种方案适用于 API 26, 即8.0（含8.0）以上可以用
                            val intent = Intent()
                            intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                            intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                            intent.putExtra(Settings.EXTRA_CHANNEL_ID, applicationInfo.uid)
                            settingActivityResult.launch(intent)
                        } else {
                            openSettingsActivity()
                        }
                    } catch (e: Exception) {
                        AppLog.put("请求通知权限出错\n$e", e, true)
                        RequestPlugins.sRequestCallback?.onError(e)
                        finish()
                    }
                }
            }

            Request.TYPE_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS -> showSettingDialog(
                permissions, rationale
            ) {
                lifecycleScope.launch {
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        intent.setData(Uri.parse("package:$packageName"))
                        val className =
                            "com.android.settings.fuelgauge.RequestIgnoreBatteryOptimizations"
                        val activities = packageManager.queryIntentActivities(
                            intent,
                            PackageManager.MATCH_DEFAULT_ONLY
                        )
                        if (activities.indexOfFirst { it.activityInfo.name == className } > -1) {
                            val component = intent.resolveActivity(packageManager)
                            if (component.className != className) {
                                intent.setClassName(
                                    "com.android.settings",
                                    className
                                )
                                settingActivityResultAwait.launch(intent)
                            }
                        }
                        intent.component = null
                        settingActivityResult.launch(intent)
                    } catch (e: Exception) {
                        AppLog.put("请求后台权限出错\n$e", e, true)
                        RequestPlugins.sRequestCallback?.onError(e)
                        finish()
                    }
                }
            }
        }
        onBackPressedDispatcher.addCallback(this) {

        }
    }

    private fun onRequestPermissionFinish() {
        RequestPlugins.sRequestCallback?.onSettingActivityResult()
        finish()
    }

    private fun openSettingsActivity() {
        try {
            val settingIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            settingIntent.data = Uri.fromParts("package", packageName, null)
            settingActivityResult.launch(settingIntent)
        } catch (e: Exception) {
            toastOnUi(R.string.tip_cannot_jump_setting_page)
            RequestPlugins.sRequestCallback?.onError(e)
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        RequestPlugins.sRequestCallback?.onRequestPermissionsResult(
            permissions,
            grantResults
        )
        finish()
    }


    override fun startActivity(intent: Intent) {
        super.startActivity(intent)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    private fun showSettingDialog(
        permissions: Array<String>,
        rationale: CharSequence?,
        onOk: () -> Unit
    ) {
        rationaleDialog?.dismiss()
        if (rationale.isNullOrEmpty()) {
            finish()
            return
        }
        rationaleDialog = AlertDialog.Builder(this)
            .setTitle(R.string.dialog_title)
            .setMessage(rationale)
            .setPositiveButton(R.string.dialog_setting) { _, _ ->
                onOk.invoke()
            }
            .setNegativeButton(R.string.dialog_cancel) { _, _ ->
                RequestPlugins.sRequestCallback?.onRequestPermissionsResult(
                    permissions,
                    IntArray(0)
                )
                finish()
            }.setOnCancelListener {
                RequestPlugins.sRequestCallback?.onRequestPermissionsResult(
                    permissions,
                    IntArray(0)
                )
                finish()
            }
            .show()
    }

    companion object {

        const val KEY_RATIONALE = "KEY_RATIONALE"
        const val KEY_INPUT_REQUEST_TYPE = "KEY_INPUT_REQUEST_TYPE"
        const val KEY_INPUT_PERMISSIONS_CODE = "KEY_INPUT_PERMISSIONS_CODE"
        const val KEY_INPUT_PERMISSIONS = "KEY_INPUT_PERMISSIONS"
    }
}
