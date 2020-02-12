package io.legado.app.ui.about

import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Html

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

class ActionItem(builder: Builder) {
    var onClickAction: ActionListener? = null
        internal set
    var onLongClickAction: ActionListener? = null
        internal set
    var text: CharSequence? = null
    @StringRes
    var textRes = 0
    var subText: CharSequence? = null
    @StringRes
    var subTextRes = 0
    var icon: Drawable? = null
    @DrawableRes
    var iconRes = 0
    private var showIcon = true

    init {
        this.text = builder.text
        this.textRes = builder.textRes

        this.subText = builder.subText
        this.subTextRes = builder.subTextRes

        this.icon = builder.icon
        this.iconRes = builder.iconRes
        this.showIcon = builder.showIcon

        this.onClickAction = builder.onClickAction
        this.onLongClickAction = builder.onLongClickAction
    }

    fun shouldShowIcon(): Boolean {
        return showIcon
    }

    class Builder {
        internal var onClickAction: ActionListener? = null
        internal var onLongClickAction: ActionListener? = null
        internal var text: CharSequence? = null
        @StringRes
        internal var textRes = 0
        internal var subText: CharSequence? = null
        @StringRes
        internal var subTextRes = 0
        internal var icon: Drawable? = null
        @DrawableRes
        internal var iconRes = 0
        internal var showIcon = true

        fun text(text: CharSequence): Builder {
            this.text = text
            this.textRes = 0
            return this
        }

        fun text(@StringRes text: Int): Builder {
            this.textRes = text
            this.text = null
            return this
        }

        fun text(textHtml: String): Builder {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                this.text = Html.fromHtml(textHtml, Html.FROM_HTML_MODE_LEGACY)
            } else {
                this.text = Html.fromHtml(textHtml)
            }
            this.textRes = 0
            return this
        }

        fun subText(subText: CharSequence): Builder {
            this.subText = subText
            this.subTextRes = 0
            return this
        }

        fun subText(@StringRes subTextRes: Int): Builder {
            this.subText = null
            this.subTextRes = subTextRes
            return this
        }

        fun subTextHtml(subTextHtml: String): Builder {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                this.subText = Html.fromHtml(subTextHtml, Html.FROM_HTML_MODE_LEGACY)
            } else {
                this.subText = Html.fromHtml(subTextHtml)
            }
            this.subTextRes = 0
            return this
        }

        fun icon(icon: Drawable): Builder {
            this.icon = icon
            this.iconRes = 0
            return this
        }

        fun icon(@DrawableRes iconRes: Int): Builder {
            this.icon = null
            this.iconRes = iconRes
            return this
        }

        fun showIcon(showIcon: Boolean): Builder {
            this.showIcon = showIcon
            return this
        }

        fun setOnClickAction(onClickAction: ActionListener): Builder {
            this.onClickAction = onClickAction
            return this
        }

        fun setOnLongClickAction(onLongClickAction: ActionListener): Builder {
            this.onLongClickAction = onLongClickAction
            return this
        }

        fun build(): ActionItem {
            return ActionItem(this)
        }
    }
}
