package io.legado.app.ui.widget.dialog

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import io.legado.app.R
import kotlinx.android.synthetic.main.dialog_text_view.*
import ru.noties.markwon.Markwon


class TextDialog : DialogFragment() {

    companion object {
        const val MD = 1

        fun show(fragmentManager: FragmentManager, content: String?, mode: Int = 0) {
            TextDialog().apply {
                val bundle = Bundle()
                bundle.putString("content", content)
                bundle.putInt("mode", mode)
                arguments = bundle
            }.show(fragmentManager, "textDialog")
        }

    }

    override fun onStart() {
        super.onStart()
        val dm = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(dm)
        dialog?.window?.setLayout((dm.widthPixels * 0.9).toInt(), (dm.heightPixels * 0.9).toInt())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_text_view, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            val content = it.getString("content") ?: ""
            when (it.getInt("mode")) {
                MD -> text_view.post {
                    Markwon.create(requireContext())
                        .setMarkdown(
                            text_view,
                            content
                        )
                }
                else -> text_view.text = content
            }
        }

    }

}
