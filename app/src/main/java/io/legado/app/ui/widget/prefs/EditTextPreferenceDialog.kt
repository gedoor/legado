package io.legado.app.ui.widget.prefs

import android.app.Dialog
import android.os.Bundle
import androidx.preference.EditTextPreferenceDialogFragmentCompat
import androidx.preference.PreferenceDialogFragmentCompat
import io.legado.app.lib.theme.ATH

class EditTextPreferenceDialog : EditTextPreferenceDialogFragmentCompat() {

    companion object {

        fun newInstance(key: String): EditTextPreferenceDialog {
            val fragment = EditTextPreferenceDialog()
            val b = Bundle(1)
            b.putString(PreferenceDialogFragmentCompat.ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawable(ATH.getDialogBackground())
        return dialog
    }

}