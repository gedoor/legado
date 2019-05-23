package io.legado.app.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import io.legado.app.R
import kotlinx.android.synthetic.main.view_titlebar.view.*

class TitleBar(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    init {
        inflate(context, R.layout.view_titlebar, this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachToActivity()
    }

    private fun attachToActivity(){
        val activity = getCompatActivity(context)
        activity?.let {
            activity.setSupportActionBar(toolbar)
            activity.supportActionBar?.let {
                it.setDisplayHomeAsUpEnabled(true)
            }
        }
    }

    private fun getCompatActivity(context: Context?): AppCompatActivity? {
        if (context == null) return null
        return when (context) {
            is AppCompatActivity -> context
            is androidx.appcompat.view.ContextThemeWrapper -> getCompatActivity(context.baseContext)
            is android.view.ContextThemeWrapper -> getCompatActivity(context.baseContext)
            else -> null
        }
    }
}