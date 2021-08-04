package io.legado.app.ui.book.group

import android.content.Context
import android.view.LayoutInflater
import io.legado.app.R
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookGroup
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.lib.dialogs.alert
import io.legado.app.utils.requestInputMethod

object GroupEdit {

    fun show(context: Context, layoutInflater: LayoutInflater, bookGroup: BookGroup) = context.run {
        alert(title = getString(R.string.group_edit)) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                textInputLayout.setHint(R.string.group_name)
                editView.setText(bookGroup.groupName)
            }
            if (bookGroup.groupId >= 0) {
                neutralButton(R.string.delete) {
                    deleteGroup(context, bookGroup)
                }
            }
            customView { alertBinding.root }
            yesButton {
                alertBinding.editView.text?.toString()?.let {
                    bookGroup.groupName = it
                    Coroutine.async {
                        appDb.bookGroupDao.update(bookGroup)
                    }
                }
            }
            noButton()
        }.show().requestInputMethod()
    }

    private fun deleteGroup(context: Context, bookGroup: BookGroup) = context.run {
        alert(R.string.delete, R.string.sure_del) {
            okButton {
                Coroutine.async {
                    appDb.bookGroupDao.delete(bookGroup)
                    val books = appDb.bookDao.getBooksByGroup(bookGroup.groupId)
                    books.forEach {
                        it.group = it.group - bookGroup.groupId
                    }
                    appDb.bookDao.update(*books.toTypedArray())
                }
            }
            noButton()
        }.show()
    }

}