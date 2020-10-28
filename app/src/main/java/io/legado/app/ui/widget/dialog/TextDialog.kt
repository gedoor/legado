package io.legado.app.ui.widget.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.utils.getSize
import io.noties.markwon.Markwon
import io.noties.markwon.image.glide.GlideImagesPlugin
import kotlinx.android.synthetic.main.dialog_text_view.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class TextDialog : BaseDialogFragment() {

    companion object {
        const val MD = 1

        fun show(
            fragmentManager: FragmentManager,
            content: String?,
            mode: Int = 0,
            time: Long = 0,
            autoClose: Boolean = false
        ) {
            TextDialog().apply {
                val bundle = Bundle()
                bundle.putString("content", content)
                bundle.putInt("mode", mode)
                bundle.putLong("time", time)
                arguments = bundle
                isCancelable = false
                this.autoClose = autoClose
            }.show(fragmentManager, "textDialog")
        }

    }

    private var time = 0L

    private var autoClose: Boolean = false

    override fun onStart() {
        super.onStart()
        val dm = requireActivity().getSize()
        dialog?.window?.setLayout((dm.widthPixels * 0.9).toInt(), (dm.heightPixels * 0.9).toInt())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_text_view, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.let {
            val content = it.getString("content") ?: ""
            when (it.getInt("mode")) {
                MD -> text_view.post {
                    Markwon.builder(requireContext())
                        .usePlugin(GlideImagesPlugin.create(requireContext()))
                        .build()
                        .setMarkdown(text_view, content)
                }
                else -> text_view.text = content
            }
            time = it.getLong("time", 0L)
        }
        if (time > 0) {
            badge_view.setBadgeCount((time / 1000).toInt())
            launch {
                while (time > 0) {
                    delay(1000)
                    time -= 1000
                    badge_view.setBadgeCount((time / 1000).toInt())
                    if (time <= 0) {
                        view.post {
                            dialog?.setCancelable(true)
                            if (autoClose) dialog?.cancel()
                        }
                    }
                }
            }
        } else {
            view.post {
                dialog?.setCancelable(true)
                if (autoClose) dialog?.cancel()
            }
        }
    }

}
