package io.legado.app.lib.permission

interface OnPermissionsResultCallback {

    fun onPermissionsGranted(requestCode: Int)

    fun onPermissionsDenied(requestCode: Int, deniedPermissions: Array<String>)

}