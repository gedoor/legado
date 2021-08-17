package io.legado.app.ui.book.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment

class SourceLoginDialog : BaseDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_login, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {

    }


}