package io.legado.app.ui.book.read.config

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment


class TocRegexDialog : BaseDialogFragment() {

    companion object {
        fun show(fragmentManager: FragmentManager, tocRegex: String? = null) {
            val dialog = TocRegexDialog()
            val bundle = Bundle()
            bundle.putString("tocRegex", tocRegex)
            dialog.arguments = bundle
            dialog.show(fragmentManager, "tocRegexDialog")
        }
    }

    override fun onStart() {
        super.onStart()
        val dm = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(dm)
        dialog?.window?.setLayout((dm.widthPixels * 0.9).toInt(), (dm.heightPixels * 0.8).toInt())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_toc_regex, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
}