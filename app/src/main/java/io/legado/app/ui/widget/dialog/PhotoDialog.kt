package io.legado.app.ui.widget.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.service.help.ReadBook
import io.legado.app.ui.book.read.page.provider.ImageProvider
import kotlinx.android.synthetic.main.dialog_photo_view.*


class PhotoDialog : BaseDialogFragment() {

    companion object {

        fun show(
            fragmentManager: FragmentManager,
            chapterIndex: Int,
            src: String,
        ) {
            PhotoDialog().apply {
                val bundle = Bundle()
                bundle.putInt("chapterIndex", chapterIndex)
                bundle.putString("src", src)
                arguments = bundle
            }.show(fragmentManager, "photoDialog")
        }

    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.dialog_photo_view, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.let {
            val chapterIndex = it.getInt("chapterIndex")
            val src = it.getString("src")
            ReadBook.book?.let { book ->
                src?.let {
                    execute {
                        ImageProvider.getImage(book, chapterIndex, src)
                    }.onSuccess { bitmap ->
                        if (bitmap != null) {
                            photo_view.setImageBitmap(bitmap)
                        }
                    }
                }
            }
        }

    }

}
