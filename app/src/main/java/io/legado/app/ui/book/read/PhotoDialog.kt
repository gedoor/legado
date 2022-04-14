package io.legado.app.ui.book.read

import android.os.Bundle
import android.view.View
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.databinding.DialogPhotoViewBinding
import io.legado.app.help.BookHelp
import io.legado.app.help.glide.ImageLoader
import io.legado.app.model.BookCover
import io.legado.app.model.ReadBook
import io.legado.app.utils.setLayout
import io.legado.app.utils.viewbindingdelegate.viewBinding

/**
 * 显示图片
 */
class PhotoDialog() : BaseDialogFragment(R.layout.dialog_photo_view) {

    constructor(chapterIndex: Int, src: String) : this() {
        arguments = Bundle().apply {
            putInt("chapterIndex", chapterIndex)
            putString("src", src)
        }
    }

    constructor(path: String) : this() {
        arguments = Bundle().apply {
            putString("path", path)
        }
    }

    private val binding by viewBinding(DialogPhotoViewBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(1f, 1f)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.let {
            val path = it.getString("path")
            if (path.isNullOrEmpty()) {
                ReadBook.book?.let { book ->
                    it.getString("src")?.let { src ->
                        val file = BookHelp.getImage(book, src)
                        ImageLoader.load(requireContext(), file)
                            .error(R.drawable.image_loading_error)
                            .into(binding.photoView)
                    }
                }
            } else {
                BookCover.load(requireContext(), path = path)
                    .into(binding.photoView)
            }
        }

    }

}
