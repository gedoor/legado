package io.legado.app.ui.book.toc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.data.appDb
import io.legado.app.data.entities.Bookmark
import io.legado.app.databinding.DialogBookmarkBinding
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookmarkDialog : BaseDialogFragment() {

    companion object {

        fun start(fragmentManager: FragmentManager, bookmark: Bookmark) {
            BookmarkDialog().apply {
                arguments = Bundle().apply {
                    putParcelable("bookmark", bookmark)
                }
            }.show(fragmentManager, "bookMarkDialog")
        }

    }

    private val binding by viewBinding(DialogBookmarkBinding::bind)

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_bookmark, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        val bookmark = arguments?.getParcelable<Bookmark>("bookmark")
        bookmark ?: let {
            dismiss()
            return
        }
        binding.run {
            tvChapterName.text = bookmark.chapterName
            editBookText.setText(bookmark.bookText)
            editContent.setText(bookmark.content)
            tvCancel.setOnClickListener {
                dismiss()
            }
            tvOk.setOnClickListener {
                bookmark.bookText = editBookText.text?.toString() ?: ""
                bookmark.content = editContent.text?.toString() ?: ""
                launch {
                    withContext(IO) {
                        appDb.bookmarkDao.insert(bookmark)
                    }
                    dismiss()
                }
            }
            tvFooterLeft.setOnClickListener {
                launch {
                    withContext(IO) {
                        appDb.bookmarkDao.delete(bookmark)
                    }
                    dismiss()
                }
            }
        }
    }


}