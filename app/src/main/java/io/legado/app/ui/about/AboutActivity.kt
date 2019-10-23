package io.legado.app.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import io.legado.app.R
import io.legado.app.base.BaseActivity
import org.jetbrains.anko.toast

class AboutActivity : BaseActivity(R.layout.activity_about) {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        val fTag = "aboutFragment"
        var aboutFragment = supportFragmentManager.findFragmentByTag(fTag)
        if (aboutFragment == null) aboutFragment = AboutFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fl_fragment, aboutFragment, fTag)
            .commit()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.about, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_scoring -> openIntent("market://details?id=$packageName")
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun openIntent(address: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(address)
            startActivity(intent)
        } catch (e: Exception) {
            toast(R.string.can_not_open)
        }
    }
}
