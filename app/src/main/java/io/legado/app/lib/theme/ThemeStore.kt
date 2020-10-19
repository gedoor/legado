package io.legado.app.lib.theme

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import androidx.annotation.AttrRes
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import io.legado.app.App
import io.legado.app.R
import io.legado.app.utils.ColorUtils

/**
 * @author Aidan Follestad (afollestad), Karim Abou Zeid (kabouzeid)
 */
@Suppress("unused")
class ThemeStore @SuppressLint("CommitPrefEdits")
private constructor(private val mContext: Context) : ThemeStoreInterface {
    private val mEditor: SharedPreferences.Editor

    init {
        mEditor = prefs(mContext).edit()
    }


    override fun primaryColor(@ColorInt color: Int): ThemeStore {
        mEditor.putInt(ThemeStorePrefKeys.KEY_PRIMARY_COLOR, color)
        if (autoGeneratePrimaryDark(mContext))
            primaryColorDark(ColorUtils.darkenColor(color))
        return this
    }

    override fun primaryColorRes(@ColorRes colorRes: Int): ThemeStore {
        return primaryColor(ContextCompat.getColor(mContext, colorRes))
    }

    override fun primaryColorAttr(@AttrRes colorAttr: Int): ThemeStore {
        return primaryColor(ATHUtils.resolveColor(mContext, colorAttr))
    }

    override fun primaryColorDark(@ColorInt color: Int): ThemeStore {
        mEditor.putInt(ThemeStorePrefKeys.KEY_PRIMARY_COLOR_DARK, color)
        return this
    }

    override fun primaryColorDarkRes(@ColorRes colorRes: Int): ThemeStore {
        return primaryColorDark(ContextCompat.getColor(mContext, colorRes))
    }

    override fun primaryColorDarkAttr(@AttrRes colorAttr: Int): ThemeStore {
        return primaryColorDark(ATHUtils.resolveColor(mContext, colorAttr))
    }

    override fun accentColor(@ColorInt color: Int): ThemeStore {
        mEditor.putInt(ThemeStorePrefKeys.KEY_ACCENT_COLOR, color)
        return this
    }

    override fun accentColorRes(@ColorRes colorRes: Int): ThemeStore {
        return accentColor(ContextCompat.getColor(mContext, colorRes))
    }

    override fun accentColorAttr(@AttrRes colorAttr: Int): ThemeStore {
        return accentColor(ATHUtils.resolveColor(mContext, colorAttr))
    }

    override fun statusBarColor(@ColorInt color: Int): ThemeStore {
        mEditor.putInt(ThemeStorePrefKeys.KEY_STATUS_BAR_COLOR, color)
        return this
    }

    override fun statusBarColorRes(@ColorRes colorRes: Int): ThemeStore {
        return statusBarColor(ContextCompat.getColor(mContext, colorRes))
    }

    override fun statusBarColorAttr(@AttrRes colorAttr: Int): ThemeStore {
        return statusBarColor(ATHUtils.resolveColor(mContext, colorAttr))
    }

    override fun navigationBarColor(@ColorInt color: Int): ThemeStore {
        mEditor.putInt(ThemeStorePrefKeys.KEY_NAVIGATION_BAR_COLOR, color)
        return this
    }

    override fun navigationBarColorRes(@ColorRes colorRes: Int): ThemeStore {
        return navigationBarColor(ContextCompat.getColor(mContext, colorRes))
    }

    override fun navigationBarColorAttr(@AttrRes colorAttr: Int): ThemeStore {
        return navigationBarColor(ATHUtils.resolveColor(mContext, colorAttr))
    }

    override fun textColorPrimary(@ColorInt color: Int): ThemeStore {
        mEditor.putInt(ThemeStorePrefKeys.KEY_TEXT_COLOR_PRIMARY, color)
        return this
    }

    override fun textColorPrimaryRes(@ColorRes colorRes: Int): ThemeStore {
        return textColorPrimary(ContextCompat.getColor(mContext, colorRes))
    }

    override fun textColorPrimaryAttr(@AttrRes colorAttr: Int): ThemeStore {
        return textColorPrimary(ATHUtils.resolveColor(mContext, colorAttr))
    }

    override fun textColorPrimaryInverse(@ColorInt color: Int): ThemeStore {
        mEditor.putInt(ThemeStorePrefKeys.KEY_TEXT_COLOR_PRIMARY_INVERSE, color)
        return this
    }

    override fun textColorPrimaryInverseRes(@ColorRes colorRes: Int): ThemeStore {
        return textColorPrimaryInverse(ContextCompat.getColor(mContext, colorRes))
    }

    override fun textColorPrimaryInverseAttr(@AttrRes colorAttr: Int): ThemeStore {
        return textColorPrimaryInverse(ATHUtils.resolveColor(mContext, colorAttr))
    }

    override fun textColorSecondary(@ColorInt color: Int): ThemeStore {
        mEditor.putInt(ThemeStorePrefKeys.KEY_TEXT_COLOR_SECONDARY, color)
        return this
    }

    override fun textColorSecondaryRes(@ColorRes colorRes: Int): ThemeStore {
        return textColorSecondary(ContextCompat.getColor(mContext, colorRes))
    }

    override fun textColorSecondaryAttr(@AttrRes colorAttr: Int): ThemeStore {
        return textColorSecondary(ATHUtils.resolveColor(mContext, colorAttr))
    }

    override fun textColorSecondaryInverse(@ColorInt color: Int): ThemeStore {
        mEditor.putInt(ThemeStorePrefKeys.KEY_TEXT_COLOR_SECONDARY_INVERSE, color)
        return this
    }

    override fun textColorSecondaryInverseRes(@ColorRes colorRes: Int): ThemeStore {
        return textColorSecondaryInverse(ContextCompat.getColor(mContext, colorRes))
    }

    override fun textColorSecondaryInverseAttr(@AttrRes colorAttr: Int): ThemeStore {
        return textColorSecondaryInverse(ATHUtils.resolveColor(mContext, colorAttr))
    }

    override fun backgroundColor(color: Int): ThemeStore {
        mEditor.putInt(ThemeStorePrefKeys.KEY_BACKGROUND_COLOR, color)
        return this
    }

    override fun bottomBackground(color: Int): ThemeStore {
        mEditor.putInt(ThemeStorePrefKeys.KEY_BOTTOM_BACKGROUND, color)
        return this
    }

    override fun coloredStatusBar(colored: Boolean): ThemeStore {
        mEditor.putBoolean(ThemeStorePrefKeys.KEY_APPLY_PRIMARYDARK_STATUSBAR, colored)
        return this
    }

    override fun coloredNavigationBar(applyToNavBar: Boolean): ThemeStore {
        mEditor.putBoolean(ThemeStorePrefKeys.KEY_APPLY_PRIMARY_NAVBAR, applyToNavBar)
        return this
    }

    override fun autoGeneratePrimaryDark(autoGenerate: Boolean): ThemeStore {
        mEditor.putBoolean(ThemeStorePrefKeys.KEY_AUTO_GENERATE_PRIMARYDARK, autoGenerate)
        return this
    }

    // Commit method

    override fun apply() {
        mEditor.putLong(ThemeStorePrefKeys.VALUES_CHANGED, System.currentTimeMillis())
            .putBoolean(ThemeStorePrefKeys.IS_CONFIGURED_KEY, true)
            .apply()
    }

    companion object {

        fun editTheme(context: Context): ThemeStore {
            return ThemeStore(context)
        }

        // Static getters

        @CheckResult
        internal fun prefs(context: Context): SharedPreferences {
            return context.getSharedPreferences(
                ThemeStorePrefKeys.CONFIG_PREFS_KEY_DEFAULT,
                Context.MODE_PRIVATE
            )
        }

        fun markChanged(context: Context) {
            ThemeStore(context).apply()
        }

        @CheckResult
        @ColorInt
        fun primaryColor(context: Context = App.INSTANCE): Int {
            return prefs(context).getInt(
                ThemeStorePrefKeys.KEY_PRIMARY_COLOR,
                ATHUtils.resolveColor(context, R.attr.colorPrimary, Color.parseColor("#455A64"))
            )
        }

        @CheckResult
        @ColorInt
        fun primaryColorDark(context: Context): Int {
            return prefs(context).getInt(
                ThemeStorePrefKeys.KEY_PRIMARY_COLOR_DARK,
                ATHUtils.resolveColor(context, R.attr.colorPrimaryDark, Color.parseColor("#37474F"))
            )
        }

        @CheckResult
        @ColorInt
        fun accentColor(context: Context = App.INSTANCE): Int {
            return prefs(context).getInt(
                ThemeStorePrefKeys.KEY_ACCENT_COLOR,
                ATHUtils.resolveColor(context, R.attr.colorAccent, Color.parseColor("#263238"))
            )
        }

        @CheckResult
        @ColorInt
        fun statusBarColor(context: Context, transparent: Boolean): Int {
            return if (!coloredStatusBar(context)) {
                Color.BLACK
            } else if (transparent) {
                prefs(context).getInt(
                    ThemeStorePrefKeys.KEY_STATUS_BAR_COLOR,
                    primaryColor(context)
                )
            } else {
                prefs(context).getInt(
                    ThemeStorePrefKeys.KEY_STATUS_BAR_COLOR,
                    primaryColorDark(context)
                )
            }
        }

        @CheckResult
        @ColorInt
        fun navigationBarColor(context: Context): Int {
            return if (!coloredNavigationBar(context)) {
                Color.BLACK
            } else prefs(context).getInt(
                ThemeStorePrefKeys.KEY_NAVIGATION_BAR_COLOR,
                bottomBackground(context)
            )
        }

        @CheckResult
        @ColorInt
        fun textColorPrimary(context: Context): Int {
            return prefs(context).getInt(
                ThemeStorePrefKeys.KEY_TEXT_COLOR_PRIMARY,
                ATHUtils.resolveColor(context, android.R.attr.textColorPrimary)
            )
        }

        @CheckResult
        @ColorInt
        fun textColorPrimaryInverse(context: Context): Int {
            return prefs(context).getInt(
                ThemeStorePrefKeys.KEY_TEXT_COLOR_PRIMARY_INVERSE,
                ATHUtils.resolveColor(context, android.R.attr.textColorPrimaryInverse)
            )
        }

        @CheckResult
        @ColorInt
        fun textColorSecondary(context: Context): Int {
            return prefs(context).getInt(
                ThemeStorePrefKeys.KEY_TEXT_COLOR_SECONDARY,
                ATHUtils.resolveColor(context, android.R.attr.textColorSecondary)
            )
        }

        @CheckResult
        @ColorInt
        fun textColorSecondaryInverse(context: Context): Int {
            return prefs(context).getInt(
                ThemeStorePrefKeys.KEY_TEXT_COLOR_SECONDARY_INVERSE,
                ATHUtils.resolveColor(context, android.R.attr.textColorSecondaryInverse)
            )
        }

        @CheckResult
        @ColorInt
        fun backgroundColor(context: Context = App.INSTANCE): Int {
            return prefs(context).getInt(
                ThemeStorePrefKeys.KEY_BACKGROUND_COLOR,
                ATHUtils.resolveColor(context, android.R.attr.colorBackground)
            )
        }

        @SuppressLint("PrivateResource")
        @CheckResult
        fun elevation(context: Context): Float {
            return prefs(context).getFloat(
                ThemeStorePrefKeys.KEY_ELEVATION,
                ATHUtils.resolveFloat(
                    context,
                    android.R.attr.elevation,
                    context.resources.getDimension(R.dimen.design_appbar_elevation)
                )
            )
        }

        @CheckResult
        @ColorInt
        fun bottomBackground(context: Context = App.INSTANCE): Int {
            return prefs(context).getInt(
                ThemeStorePrefKeys.KEY_BOTTOM_BACKGROUND,
                ATHUtils.resolveColor(context, android.R.attr.colorBackground)
            )
        }

        @CheckResult
        fun coloredStatusBar(context: Context): Boolean {
            return prefs(context).getBoolean(
                ThemeStorePrefKeys.KEY_APPLY_PRIMARYDARK_STATUSBAR,
                true
            )
        }

        @CheckResult
        fun coloredNavigationBar(context: Context): Boolean {
            return prefs(context).getBoolean(ThemeStorePrefKeys.KEY_APPLY_PRIMARY_NAVBAR, false)
        }

        @CheckResult
        fun autoGeneratePrimaryDark(context: Context): Boolean {
            return prefs(context).getBoolean(ThemeStorePrefKeys.KEY_AUTO_GENERATE_PRIMARYDARK, true)
        }

        @CheckResult
        fun isConfigured(context: Context): Boolean {
            return prefs(context).getBoolean(ThemeStorePrefKeys.IS_CONFIGURED_KEY, false)
        }

        @SuppressLint("CommitPrefEdits")
        fun isConfigured(context: Context, version: Int): Boolean {
            val prefs = prefs(context)
            val lastVersion = prefs.getInt(ThemeStorePrefKeys.IS_CONFIGURED_VERSION_KEY, -1)
            if (version > lastVersion) {
                prefs.edit().putInt(ThemeStorePrefKeys.IS_CONFIGURED_VERSION_KEY, version).apply()
                return false
            }
            return true
        }
    }
}