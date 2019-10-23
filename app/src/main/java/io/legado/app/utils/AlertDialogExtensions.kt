package io.legado.app.utils

import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import io.legado.app.lib.theme.ATH

fun AlertDialog.applyTint(): AlertDialog {
    return ATH.setAlertDialogTint(this)
}

fun AlertDialog.requestInputMethod(){
    window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
}
