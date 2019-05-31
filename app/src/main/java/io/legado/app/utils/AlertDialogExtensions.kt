package io.legado.app.utils

import androidx.appcompat.app.AlertDialog
import io.legado.app.lib.theme.ATH

val AlertDialog.upTint: AlertDialog
    get() = ATH.setAlertDialogTint(this)