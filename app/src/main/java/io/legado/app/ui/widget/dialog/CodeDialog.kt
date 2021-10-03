package io.legado.app.ui.widget.dialog

import android.os.Bundle
import android.view.View
import io.legado.app.base.BaseDialogFragment
import io.legado.app.utils.setLayout

class CodeDialog : BaseDialogFragment() {

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, 0.9f)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {

    }

}