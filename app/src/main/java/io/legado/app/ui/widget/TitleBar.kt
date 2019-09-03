package io.legado.app.ui.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Menu
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.appbar.AppBarLayout
import io.legado.app.R
import io.legado.app.lib.theme.DrawableUtils
import io.legado.app.lib.theme.primaryColor
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.utils.activity
import io.legado.app.utils.getNavigationBarHeight
import io.legado.app.utils.getStatusBarHeight
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.bottomPadding
import org.jetbrains.anko.topPadding

class TitleBar(context: Context, attrs: AttributeSet?) : AppBarLayout(context, attrs) {

    val toolbar: Toolbar
    val menu: Menu
        get() = toolbar.menu

    var title: CharSequence?
        get() = toolbar.title
        set(title) {
            toolbar.title = title
        }

    var subtitle: CharSequence?
        get() = toolbar.subtitle
        set(subtitle) {
            toolbar.subtitle = subtitle
        }

    private val displayHomeAsUp: Boolean
    private val navigationIconTint: ColorStateList?
    private val navigationIconTintMode: Int
    private val attachToActivity: Boolean

    init {
        inflate(context, R.layout.view_title_bar, this)
        toolbar = findViewById(R.id.toolbar)
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.TitleBar,
            R.attr.titleBarStyle, 0
        )
        navigationIconTint = a.getColorStateList(R.styleable.TitleBar_navigationIconTint)
        navigationIconTintMode = a.getInt(R.styleable.TitleBar_navigationIconTintMode, 9)
        attachToActivity = a.getBoolean(R.styleable.TitleBar_attachToActivity, true)
        displayHomeAsUp = a.getBoolean(R.styleable.TitleBar_displayHomeAsUp, true)

        val navigationIcon = a.getDrawable(R.styleable.TitleBar_navigationIcon)
        val navigationContentDescription =
            a.getText(R.styleable.TitleBar_navigationContentDescription)
        val titleText = a.getString(R.styleable.TitleBar_title)
        val subtitleText = a.getString(R.styleable.TitleBar_subtitle)

        toolbar.apply {
            navigationIcon?.let {
                this.navigationIcon = it
                this.navigationContentDescription = navigationContentDescription
            }

            if (a.hasValue(R.styleable.TitleBar_titleTextAppearance)) {
                this.setTitleTextAppearance(
                    context,
                    a.getResourceId(R.styleable.TitleBar_titleTextAppearance, 0)
                )
            }

            if (a.hasValue(R.styleable.TitleBar_titleTextColor)) {
                this.setTitleTextColor(a.getColor(R.styleable.TitleBar_titleTextColor, -0x1))
            }

            if (a.hasValue(R.styleable.TitleBar_subtitleTextAppearance)) {
                this.setSubtitleTextAppearance(
                    context,
                    a.getResourceId(R.styleable.TitleBar_subtitleTextAppearance, 0)
                )
            }

            if (a.hasValue(R.styleable.TitleBar_subtitleTextColor)) {
                this.setSubtitleTextColor(a.getColor(R.styleable.TitleBar_subtitleTextColor, -0x1))
            }


            if (a.hasValue(R.styleable.TitleBar_contentInsetLeft)
                || a.hasValue(R.styleable.TitleBar_contentInsetRight)
            ) {
                this.setContentInsetsAbsolute(
                    a.getDimensionPixelSize(R.styleable.TitleBar_contentInsetLeft, 0),
                    a.getDimensionPixelSize(R.styleable.TitleBar_contentInsetRight, 0)
                )
            }

            if (a.hasValue(R.styleable.TitleBar_contentInsetStart)
                || a.hasValue(R.styleable.TitleBar_contentInsetEnd)
            ) {
                this.setContentInsetsRelative(
                    a.getDimensionPixelSize(R.styleable.TitleBar_contentInsetStart, 0),
                    a.getDimensionPixelSize(R.styleable.TitleBar_contentInsetEnd, 0)
                )
            }

            if (a.hasValue(R.styleable.TitleBar_contentInsetStartWithNavigation)) {
                this.contentInsetStartWithNavigation = a.getDimensionPixelOffset(
                    R.styleable.TitleBar_contentInsetStartWithNavigation, 0
                )
            }

            if (a.hasValue(R.styleable.TitleBar_contentInsetEndWithActions)) {
                this.contentInsetEndWithActions = a.getDimensionPixelOffset(
                    R.styleable.TitleBar_contentInsetEndWithActions, 0
                )
            }

            if (!titleText.isNullOrBlank()) {
                this.title = titleText
            }

            if (!subtitleText.isNullOrBlank()) {
                this.subtitle = subtitleText
            }

            if (a.hasValue(R.styleable.TitleBar_contentLayout)) {
                inflate(context, a.getResourceId(R.styleable.TitleBar_contentLayout, 0), this)
            }
        }

        if (a.getBoolean(R.styleable.TitleBar_fitStatusBar, true)) {
            topPadding = context.getStatusBarHeight()
        }

        if (a.getBoolean(R.styleable.TitleBar_fitNavigationBar, false)) {
            bottomPadding = context.getNavigationBarHeight()
        }

        backgroundColor = context.primaryColor

        a.recycle()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachToActivity()
    }

    fun setNavigationOnClickListener(clickListener: ((View) -> Unit)) {
        toolbar.setNavigationOnClickListener(clickListener)
    }

    fun setTitle(titleId: Int) {
        toolbar.setTitle(titleId)
    }

    fun setSubTitle(subtitleId: Int) {
        toolbar.setSubtitle(subtitleId)
    }

    fun setTitleTextColor(@ColorInt color: Int) {
        toolbar.setTitleTextColor(color)
    }

    fun setTitleTextAppearance(@StyleRes resId: Int) {
        toolbar.setTitleTextAppearance(context, resId)
    }

    fun setSubTitleTextColor(@ColorInt color: Int) {
        toolbar.setSubtitleTextColor(color)
    }

    fun setSubTitleTextAppearance(@StyleRes resId: Int) {
        toolbar.setSubtitleTextAppearance(context, resId)
    }

    private fun attachToActivity() {
        if (attachToActivity) {
            activity?.let {
                it.setSupportActionBar(toolbar)
                it.supportActionBar?.setDisplayHomeAsUpEnabled(displayHomeAsUp)
            }
        }

        val primaryTextColor = if (isInEditMode) Color.BLACK else context.primaryTextColor
        DrawableUtils.setTint(toolbar.overflowIcon, primaryTextColor)
        toolbar.setTitleTextColor(primaryTextColor)

        if (navigationIconTint != null) {
            wrapDrawableTint(toolbar.navigationIcon, navigationIconTint, navigationIconTintMode)
        } else {
            wrapDrawableTint(
                toolbar.navigationIcon,
                ColorStateList.valueOf(primaryTextColor),
                navigationIconTintMode
            )
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