package io.legado.app.ui.filechooser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

}