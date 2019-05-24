package io.legado.app.help.permission

interface OnPermissionsResultCallback {

    fun onPermissionsGranted(requestCode: Int)

    fun onPermissionsDenied(requestCode: Int, deniedPermissions: Array<String>)

}