package io.legado.app.utils

import androidx.appcompat.app.AlertDialog
import io.legado.app.lib.theme.ATH

fun AlertDialog.upTint(): AlertDialog {
    return ATH.setAlertDialogTint(this)
}
