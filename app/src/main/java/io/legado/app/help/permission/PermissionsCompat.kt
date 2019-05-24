package io.legado.app.help.permission

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class PermissionsCompat private constructor() {

    private var mRequest: Request? = null

    fun request() {
        RequestManager.pushRequest(mRequest)
    }

    class Builder {
        private val mRequest: Request

        constructor(activity: AppCompatActivity) {
            mRequest = Request(activity)
        }

        constructor(fragment: Fragment) {
            mRequest = Request(fragment)
        }

        fun addPermissions(vararg permissions: String): Builder {
            mRequest.addPermissions(*permissions)
            return this
        }

        fun requestCode(requestCode: Int): Builder {
            mRequest.setRequestCode(requestCode)
            return this
        }

        fun onGranted(callback: (requestCode: Int) -> Unit): Builder {
            mRequest.setOnGrantedCallback(object : OnPermissionsGrantedCallback {
                override fun onPermissionsGranted(requestCode: Int) {
                    callback(requestCode)
                }
            })
            return this
        }

        fun onDenied(callback: (requestCode: Int, deniedPermissions: Array<String>) -> Unit): Builder {
            mRequest.setOnDeniedCallback(object : OnPermissionsDeniedCallback {
                override fun onPermissionsDenied(requestCode: Int, deniedPermissions: Array<String>) {
                    callback(requestCode, deniedPermissions)
                }
            })
            return this
        }

        fun rationale(rationale: CharSequence): Builder {
            mRequest.setRationale(rationale)
            return this
        }

        fun rationale(@StringRes resId: Int): Builder {
            mRequest.setRationale(resId)
            return this
        }

        fun build(): PermissionsCompat {
            val compat = PermissionsCompat()
            compat.mRequest = mRequest
            return compat
        }

        fun request(): PermissionsCompat {
            val compat = build()
            compat.mRequest = mRequest
            compat.request()
            return compat
        }
    }

}
