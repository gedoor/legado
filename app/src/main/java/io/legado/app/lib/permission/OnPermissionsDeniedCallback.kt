package io.legado.app.lib.permission

interface OnPermissionsDeniedCallback {

    fun onPermissionsDenied(requestCode: Int, deniedPermissions: Array<String>)

}
