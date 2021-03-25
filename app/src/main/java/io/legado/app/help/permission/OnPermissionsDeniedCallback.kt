package io.legado.app.help.permission

interface OnPermissionsDeniedCallback {

    fun onPermissionsDenied(requestCode: Int, deniedPermissions: Array<String>)

}
