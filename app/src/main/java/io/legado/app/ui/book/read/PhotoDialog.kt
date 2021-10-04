package io.legado.app.ui.book.read

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.databinding.DialogPhotoViewBinding
import io.legado.app.model.ReadBook
import io.legado.app.ui.book.read.page.provider.ImageProvider
import io.legado.app.utils.setLayout
import io.legado.app.utils.viewbindingdelegate.viewBinding


class PhotoDialog() : BaseDialogFragment(R.layout.dialog_photo_view) {

    constructor(chapterIndex: Int, src: String) : this() {
        arguments = Bundle().apply {
            putInt("chapterIndex", chapterIndex)
            putString("src", src)
        }
    }

    private val binding by viewBinding(DialogPhotoViewBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.let {
            val chapterIndex = it.getInt("chapterIndex")
            val src = it.getString("src")
            ReadBook.book?.let { book ->
                src?.let {
                    execute {
                        ImageProvider.getImage(book, chapterIndex, src, ReadBook.bookSource)
                    }.onSuccess { bitmap ->
                        if (bitmap != null) {
                            binding.photoView.setImageBitmap(bitmap)
                        }
                    }
                }
            }
        }

    }

}
