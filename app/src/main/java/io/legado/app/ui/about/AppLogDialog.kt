package io.legado.app.ui.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.utils.windowSize

class AppLogDialog : BaseDialogFragment() {

    companion object {
        fun show(fragmentManager: FragmentManager) {
            AppLogDialog().show(fragmentManager, "appLogDialog")
        }
    }

    override fun onStart() {
        super.onStart()
        val dm = requireActivity().windowSize
        dialog?.window?.setLayout(
            (dm.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_book_group_edit, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {

    }

}