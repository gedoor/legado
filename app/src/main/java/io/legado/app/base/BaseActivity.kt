package io.legado.app.base

import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {

    @LayoutRes
    abstract fun getLayoutID(): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutID())
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.let {
            if (it.itemId == android.R.id.home) {
                supportFinishAfterTransition()
                return true
            }
        }
        return if (item == null) true else onCompatOptionsItemSelected(item)
    }

    open fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        return true
    }

    override fun setTitle(title: CharSequence?) {
        supportActionBar?.let {
            it.title = title
        }
    }

    override fun setTitle(titleId: Int) {
        supportActionBar?.let {
            it.setTitle(titleId)
        }
    }

    fun setSubTitle(subtitle: CharSequence?){
        supportActionBar?.let {
            it.subtitle = subtitle;
        }
    }

    fun setSubTitle(subtitleId: Int){
        supportActionBar?.let {
            it.setSubtitle(subtitleId)
        }
    }
}