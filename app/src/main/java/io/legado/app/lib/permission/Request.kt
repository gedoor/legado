package io.legado.app.lib.permission

import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import io.legado.app.R
import io.legado.app.utils.startActivity
import java.util.*

@Suppress("MemberVisibilityCanBePrivate")
internal class Request : OnRequestPermissionsResultCallback {

    internal val requestTime: Long
    private var requestCode: Int = TYPE_REQUEST_PERMISSION
    private var source: RequestSource? = null
    private var permissions: ArrayList<String>? = null
    private var grantedCallback: OnPermissionsGrantedCallback? = null
    private var deniedCallback: OnPermissionsDeniedCallback? = null
    private var rationaleResId: Int = 0
    private var rationale: CharSequence? = null

    private var rationaleDialog: AlertDialog? = null

    private val deniedPermissions: Array<String>?
        get() {
            return getDeniedPermissions(this.permissions?.toTypedArray())
        }

    constructor(activity: AppCompatActivity) {
        source = ActivitySource(activity)
        permissions = ArrayList()
        requestTime = System.currentTimeMillis()
    }

    constructor(fragment: Fragment) {
        source = FragmentSource(fragment)
        permissions = ArrayList()
        requestTime = System.currentTimeMillis()
    }

    fun addPermissions(vararg permissions: String) {
        this.permissions?.addAll(listOf(*permissions))
    }

    fun setRequestCode(requestCode: Int) {
        this.requestCode = requestCode
    }

    fun setOnGrantedCallback(callback: OnPermissionsGrantedCallback) {
        grantedCallback = callback
    }

    fun setOnDeniedCallback(callback: OnPermissionsDeniedCallback) {
        deniedCallback = callback
    }

    fun setRationale(@StringRes resId: Int) {
        rationaleResId = resId
        rationale = null
    }

    fun setRationale(rationale: CharSequence) {
        this.rationale = rationale
        rationaleResId = 0
    }

    fun start() {
        RequestPlugins.setOnRequestPermissionsCallback(this)

        val deniedPermissions = deniedPermissions

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (deniedPermissions == null) {
                onPermissionsGranted()
            } else {
                val rationale =
                    if (rationaleResId != 0) source?.context?.getText(rationaleResId) else rationale
                if (rationale != null) {
                    showSettingDialog(rationale) {
                        onPermissionsDenied(
                            requestCode,
                            deniedPermissions
                        )
                    }
                } else {
                    onPermissionsDenied(requestCode, deniedPermissions)
                }
            }
        } else {
            if (deniedPermissions != null) {
                source?.context?.startActivity<PermissionActivity> {
                    putExtra(PermissionActivity.KEY_INPUT_REQUEST_TYPE, TYPE_REQUEST_PERMISSION)
                    putExtra(PermissionActivity.KEY_INPUT_PERMISSIONS_CODE, requestCode)
                    putExtra(PermissionActivity.KEY_INPUT_PERMISSIONS, deniedPermissions)
                }
            } else {
                onPermissionsGranted()
            }
        }
    }

    fun clear() {
        grantedCallback = null
        deniedCallback = null
    }

    fun getDeniedPermissions(permissions: Array<String>?): Array<String>? {
        if (permissions != null) {
            val deniedPermissionList = ArrayList<String>()
            for (permission in permissions) {
                if (source?.context?.let {
                        ContextCompat.checkSelfPermission(
                            it,
                            permission
                        )
                    } != PackageManager.PERMISSION_GRANTED
                ) {
                    deniedPermissionList.add(permission)
                }
            }
            val size = deniedPermissionList.size
            if (size > 0) {
                return deniedPermissionList.toTypedArray()
            }
        }
        return null
    }

    private fun showSettingDialog(rationale: CharSequence, cancel: () -> Unit) {
        rationaleDialog?.dismiss()
        source?.context?.let {
            runCatching {
                rationaleDialog = AlertDialog.Builder(it)
                    .setTitle(R.string.dialog_title)
                    .setMessage(rationale)
                    .setPositiveButton(R.string.dialog_setting) { _, _ ->
                        it.startActivity<PermissionActivity> {
                            putExtra(
                                PermissionActivity.KEY_INPUT_REQUEST_TYPE,
                                TYPE_REQUEST_SETTING
                            )
                        }
                    }
                    .setNegativeButton(R.string.dialog_cancel) { _, _ -> cancel() }
                    .show()
            }
        }
    }

    private fun onPermissionsGranted() {
        try {
            grantedCallback?.onPermissionsGranted()
        } catch (ignore: Exception) {
        }

        RequestPlugins.sResultCallback?.onPermissionsGranted()
    }

    private fun onPermissionsDenied(requestCode: Int, deniedPermissions: Array<String>) {
        try {
            deniedCallback?.onPermissionsDenied(deniedPermissions)
        } catch (ignore: Exception) {
        }

        RequestPlugins.sResultCallback?.onPermissionsDenied(deniedPermissions)
    }

    override fun onRequestPermissionsResult(
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        val deniedPermissions = getDeniedPermissions(permissions)
        if (deniedPermissions != null) {
            val rationale =
                if (rationaleResId != 0) source?.context?.getText(rationaleResId) else rationale
            if (rationale != null) {
                showSettingDialog(rationale) { onPermissionsDenied(requestCode, deniedPermissions) }
            } else {
                onPermissionsDenied(requestCode, deniedPermissions)
            }
        } else {
            onPermissionsGranted()
        }
    }

    override fun onSettingActivityResult() {
        val deniedPermissions = deniedPermissions
        if (deniedPermissions == null) {
            onPermissionsGranted()
        } else {
            onPermissionsDenied(this.requestCode, deniedPermissions)
        }
    }

    companion object {
        const val TYPE_REQUEST_PERMISSION = 1
        const val TYPE_REQUEST_SETTING = 2
    }
}