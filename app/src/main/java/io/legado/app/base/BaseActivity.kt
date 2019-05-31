package io.legado.app.base

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

abstract class BaseActivity<BD : ViewDataBinding, VM : ViewModel?> : AppCompatActivity() {

    protected lateinit var dataBinding: BD
        private set

    protected abstract val viewModel: VM

    protected abstract val layoutID: Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataBinding = DataBindingUtil.setContentView(this, layoutID)
        onViewModelCreated(viewModel, savedInstanceState)
    }

    open fun onViewModelCreated(viewModel: VM, savedInstanceState: Bundle?) {

    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.let {
            if (it.itemId == android.R.id.home) {
                supportFinishAfterTransition()
                return true
            }
        }
        return item != null && onCompatOptionsItemSelected(item)
    }

    open fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        return true
    }
}