package io.legado.app.ui.widget.page

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem

import io.legado.app.R
import io.legado.app.constant.Bus
import io.legado.app.utils.postEvent

class ContentSelectActionCallback(private val textView: ContentTextView) : ActionMode.Callback {

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_replace -> {
                val text = textView.text.substring(textView.selectionStart, textView.selectionEnd)
                postEvent(Bus.REPLACE, text)
                mode?.finish()
                return true
            }
        }
        return false
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode?.menuInflater?.inflate(R.menu.content_select_action, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return false
    }

    override fun onDestroyActionMode(mode: ActionMode?) {

    }

}