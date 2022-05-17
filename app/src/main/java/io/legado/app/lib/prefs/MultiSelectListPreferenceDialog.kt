package io.legado.app.lib.prefs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.preference.MultiSelectListPreferenceDialogFragmentCompat
import androidx.preference.PreferenceDialogFragmentCompat
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.filletBackground

class MultiSelectListPreferenceDialog : MultiSelectListPreferenceDialogFragmentCompat() {

    companion object {

        fun newInstance(key: String?): MultiSelectListPreferenceDialog {
            val fragment =
                MultiSelectListPreferenceDialog()
            val b = Bundle(1)
            b.putString(PreferenceDialogFragmentCompat.ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }

    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawable(requireContext().filletBackground)
        dialog.window?.decorView?.post {
            (dialog as AlertDialog).run {
                getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(accentColor)
                getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(accentColor)
                getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(accentColor)
            }
        }
        return dialog
    }


}