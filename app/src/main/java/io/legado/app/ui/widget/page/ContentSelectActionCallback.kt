package io.legado.app.ui.widget.page

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem

import io.legado.app.R

object ContentSelectActionCallback : ActionMode.Callback {

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_replace -> {
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