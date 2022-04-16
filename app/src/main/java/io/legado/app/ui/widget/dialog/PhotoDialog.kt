package io.legado.app.ui.widget.dialog

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

    constructor(src: String) : this() {
        arguments = Bundle().apply {
            putString("src", src)
        }
    }

    private val binding by viewBinding(DialogPhotoViewBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(1f, 1f)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.getString("src")?.let { src ->
            val file = ReadBook.book?.let { book ->
                BookHelp.getImage(book, src)
            }
            if (file?.exists() == true) {
                ImageLoader.load(requireContext(), file)
                    .error(R.drawable.image_loading_error)
                    .into(binding.photoView)
            } else {
                BookCover.load(requireContext(), path = src)
                    .into(binding.photoView)
            }
        }

    }

}
