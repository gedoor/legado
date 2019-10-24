package io.legado.app.ui.filechooser

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager


class FileChooserDialog : DialogFragment() {

    companion object {
        const val tag = "FileChooserDialog"

        fun show(manager: FragmentManager) {
            val fragment =
                (manager.findFragmentByTag(tag) as? FileChooserDialog) ?: FileChooserDialog()
            fragment.show(manager, tag)
        }
    }


}