package io.legado.app.lib.permission

import androidx.annotation.StringRes

@Suppress("unused")
class PermissionsCompat private constructor() {

    private var request: Request? = null

    fun request() {
        RequestManager.pushRequest(request)
    }

    class Builder {
        private val request: Request = Request()

        fun addPermissions(vararg permissions: String): Builder {
            request.addPermissions(*permissions)
            return this
        }

        fun onGranted(callback: () -> Unit): Builder {
            request.setOnGrantedCallback(object : OnPermissionsGrantedCallback {
                override fun onPermissionsGranted() {
                    callback()
                }
            })
            return this
        }

        fun onDenied(callback: (deniedPermissions: Array<String>) -> Unit): Builder {
            request.setOnDeniedCallback(object : OnPermissionsDeniedCallback {
                override fun onPermissionsDenied(deniedPermissions: Array<String>) {
                    callback(deniedPermissions)
                }
            })
            return this
        }

        fun onError(callback: (e: Exception) -> Unit): Builder {
            request.setOnErrorCallBack(object : OnErrorCallback{
                override fun onError(e: Exception) {
                    callback(e)
                }
            })
            return this
        }

        fun rationale(rationale: CharSequence): Builder {
            request.setRationale(rationale)
            return this
        }

        fun rationale(@StringRes resId: Int): Builder {
            request.setRationale(resId)
            return this
        }

        fun build(): PermissionsCompat {
            val compat = PermissionsCompat()
            compat.request = request
            return compat
        }

        fun request(): PermissionsCompat {
            val compat = build()
            compat.request()
            return compat
        }
    }

}
