package io.legado.app.ui.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Menu
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.appbar.AppBarLayout
import io.legado.app.R
import kotlinx.android.synthetic.main.view_titlebar.view.*

class TitleBar(context: Context, attrs: AttributeSet?) : AppBarLayout(context, attrs) {

    val toolbar: Toolbar
    val menu: Menu
        get() = toolbar.menu

    init {
        inflate(context, R.layout.view_titlebar, this)
        toolbar = findViewById(R.id.toolbar)
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.TitleBar,
            R.attr.titleBarStyle, 0
        )
        val navigationIcon = a.getDrawable(R.styleable.TitleBar_navigationIcon)
        val navigationContentDescription = a.getText(R.styleable.TitleBar_navigationContentDescription)
        val navigationIconTint = a.getColorStateList(R.styleable.TitleBar_navigationIconTint)
        val navigationIconTintMode = a.getInt(R.styleable.TitleBar_navigationIconTintMode, 9)
        val showNavigationIcon = a.getBoolean(R.styleable.TitleBar_showNavigationIcon, true)
        val attachToActivity = a.getBoolean(R.styleable.TitleBar_attachToActivity, true)
        val titleText = a.getString(R.styleable.TitleBar_title)
        val subtitleText = a.getString(R.styleable.TitleBar_subtitle)

        toolbar.apply {
            if(showNavigationIcon){
                this.navigationIcon = navigationIcon
                this.navigationContentDescription = navigationContentDescription
                wrapDrawableTint(this.navigationIcon, navigationIconTint, navigationIconTintMode)
            }

            if (a.hasValue(R.styleable.TitleBar_titleTextAppearance)) {
                this.setTitleTextAppearance(context, a.getResourceId(R.styleable.TitleBar_titleTextAppearance, 0))
            }

            if (a.hasValue(R.styleable.TitleBar_titleTextColor)) {
                this.setTitleTextColor(a.getColor(R.styleable.TitleBar_titleTextColor, -0x1))
            }

            if (a.hasValue(R.styleable.TitleBar_subtitleTextAppearance)) {
                this.setSubtitleTextAppearance(context, a.getResourceId(R.styleable.TitleBar_subtitleTextAppearance, 0))
            }

            if (a.hasValue(R.styleable.TitleBar_subtitleTextColor)) {
                this.setSubtitleTextColor(a.getColor(R.styleable.TitleBar_subtitleTextColor, -0x1))
            }

            if(!titleText.isNullOrBlank()){
                this.title = titleText
            }

            if(!subtitleText.isNullOrBlank()){
                this.subtitle = subtitleText
            }
        }

        a.recycle()

        if (attachToActivity) {
            attachToActivity(context)
        }
    }

    fun setNavigationOnClickListener(clickListener: ((View) -> Unit)){
        toolbar.setNavigationOnClickListener(clickListener)
    }

    fun setTitle(title: CharSequence?) {
        toolbar.title = title
    }

    fun setTitle(titleId: Int) {
        toolbar.setTitle(titleId)
    }

    fun setSubTitle(subtitle: CharSequence?) {
        toolbar.subtitle = subtitle
    }

    fun setSubTitle(subtitleId: Int) {
        toolbar.setSubtitle(subtitleId)
    }

    fun setTitleTextColor(@ColorInt color: Int){
        toolbar.setTitleTextColor(color)
    }

    fun setTitleTextAppearance(@StyleRes resId: Int){
        toolbar.setTitleTextAppearance(context, resId)
    }

    fun setSubTitleTextColor(@ColorInt color: Int){
        toolbar.setSubtitleTextColor(color)
    }

    fun setSubTitleTextAppearance(@StyleRes resId: Int){
        toolbar.setSubtitleTextAppearance(context, resId)
    }

    private fun attachToActivity(context: Context) {
        val activity = getCompatActivity(context)
        activity?.let {
            activity.setSupportActionBar(toolbar)
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
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

    private fun wrapDrawableTint(drawable: Drawable?, tintList: ColorStateList?, tintMode: Int) {
        if (drawable == null || tintList == null) return
        val wrappedDrawable = DrawableCompat.wrap(drawable.mutate())
        DrawableCompat.setTintList(wrappedDrawable, tintList)
        DrawableCompat.setTintMode(wrappedDrawable, intToMode(tintMode))
    }

    private fun intToMode(`val`: Int): PorterDuff.Mode {
        when (`val`) {
            0 -> return PorterDuff.Mode.CLEAR
            1 -> return PorterDuff.Mode.SRC
            2 -> return PorterDuff.Mode.DST
            3 -> return PorterDuff.Mode.SRC_OVER
            4 -> return PorterDuff.Mode.DST_OVER
            5 -> return PorterDuff.Mode.SRC_IN
            6 -> return PorterDuff.Mode.DST_IN
            7 -> return PorterDuff.Mode.SRC_OUT
            8 -> return PorterDuff.Mode.DST_OUT
            9 -> return PorterDuff.Mode.SRC_ATOP
            10 -> return PorterDuff.Mode.DST_ATOP
            11 -> return PorterDuff.Mode.XOR
            16 -> return PorterDuff.Mode.DARKEN
            17 -> return PorterDuff.Mode.LIGHTEN
            13 -> return PorterDuff.Mode.MULTIPLY
            14 -> return PorterDuff.Mode.SCREEN
            12 -> return PorterDuff.Mode.ADD
            15 -> return PorterDuff.Mode.OVERLAY
            else -> return PorterDuff.Mode.CLEAR
        }
    }
}