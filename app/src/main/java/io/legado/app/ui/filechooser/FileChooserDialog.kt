package io.legado.app.ui.filechooser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import io.legado.app.R


class FileChooserDialog : DialogFragment() {

    companion object {
        const val tag = "FileChooserDialog"

        fun show(manager: FragmentManager) {
            val fragment =
                (manager.findFragmentByTag(tag) as? FileChooserDialog) ?: FileChooserDialog()
            fragment.show(manager, tag)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_file_chooser, container, true)
    }

}