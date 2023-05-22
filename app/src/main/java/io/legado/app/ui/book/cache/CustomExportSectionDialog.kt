package io.legado.app.ui.book.cache

import android.os.Bundle
import android.view.View
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.databinding.DialogSelectSectionExportBinding
import io.legado.app.utils.viewbindingdelegate.viewBinding

class CustomExportSectionDialog() : BaseDialogFragment(R.layout.dialog_select_section_export) {

    private val binding by viewBinding(DialogSelectSectionExportBinding::bind)
    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        TODO("Not yet implemented")
    }
}