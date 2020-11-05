package io.legado.app.ui.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.Menu
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.appcompat.widget.Toolbar
import com.google.android.material.appbar.AppBarLayout
import io.legado.app.R
import io.legado.app.help.AppConfig
import io.legado.app.lib.theme.elevation
import io.legado.app.lib.theme.primaryColor
import io.legado.app.utils.activity
import io.legado.app.utils.navigationBarHeight
import io.legado.app.utils.statusBarHeight
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.bottomPadding
import org.jetbrains.anko.topPadding

@Suppress("unused")
class TitleBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppBarLayout(context, attrs) {

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

        when (a.getInt(R.styleable.TitleBar_themeMode, 0)) {
            1 -> inflate(context, R.layout.view_title_bar_dark, this)
            else -> inflate(context, R.layout.view_title_bar, this)
        }
        toolbar = findViewById(R.id.toolbar)

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

        if (!isInEditMode) {
            if (a.getBoolean(R.styleable.TitleBar_fitStatusBar, true)) {
                topPadding = context.statusBarHeight
            }

            if (a.getBoolean(R.styleable.TitleBar_fitNavigationBar, false)) {
                bottomPadding = context.navigationBarHeight
            }

            backgroundColor = context.primaryColor

            stateListAnimator = null
            elevation = if (AppConfig.elevation < 0) {
                context.elevation
            } else {
                AppConfig.elevation.toFloat()
            }
        }
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

    fun transparent() {
        elevation = 0f
        backgroundColor = Color.TRANSPARENT
    }

    fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, fullScreen: Boolean) {
        topPadding = if (!isInMultiWindowMode && fullScreen) context.statusBarHeight else 0
    }

    private fun attachToActivity() {
        if (attachToActivity) {
            activity?.let {
                it.setSupportActionBar(toolbar)
                it.supportActionBar?.setDisplayHomeAsUpEnabled(displayHomeAsUp)
            }
        }
    }

}