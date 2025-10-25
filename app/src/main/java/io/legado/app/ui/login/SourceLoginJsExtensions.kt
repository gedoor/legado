package io.legado.app.ui.login

import io.legado.app.data.entities.BaseSource
import io.legado.app.help.JsExtensions
import io.legado.app.ui.association.AddToBookshelfDialog
import io.legado.app.ui.book.search.SearchActivity
import io.legado.app.utils.sendToClip
import io.legado.app.utils.showDialogFragment

class SourceLoginJsExtensions(private val fragment: SourceLoginDialog, private val source: BaseSource?) : JsExtensions {
    override fun getSource(): BaseSource? {
        return source
    }

    fun searchBook(key: String) {
        searchBook(key, null)
    }

    fun searchBook(key: String, searchScope: String?) {
        SearchActivity.start(fragment.requireContext(), key, searchScope)
    }

    fun addBook(bookUrl: String) {
        fragment.showDialogFragment(AddToBookshelfDialog(bookUrl))
    }

    fun copyText(text: String) {
        fragment.requireContext().sendToClip(text)
    }
}