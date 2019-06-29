package io.legado.app.lib.theme

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
import android.widget.EdgeEffect
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.utils.getPrefBoolean


/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object ATH {

    @SuppressLint("CommitPrefEdits")
    fun didThemeValuesChange(context: Context, since: Long): Boolean {
        return ThemeStore.isConfigured(context) && ThemeStore.prefs(context).getLong(
            ThemeStorePrefKeys.VALUES_CHANGED,
            -1
        ) > since
    }

    fun setStatusbarColorAuto(activity: Activity) {
        setStatusbarColor(activity, ThemeStore.statusBarColor(activity, activity.getPrefBoolean("transparentStatusBar")))
    }

    fun setStatusbarColor(activity: Activity, color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.window.statusBarColor = color
            setLightStatusbarAuto(activity, color)
        }
    }

    fun setLightStatusbarAuto(activity: Activity, bgColor: Int) {
        setLightStatusbar(activity, ColorUtils.isColorLight(bgColor))
    }

    fun setLightStatusbar(activity: Activity, enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val decorView = activity.window.decorView
            val systemUiVisibility = decorView.systemUiVisibility
            if (enabled) {
                decorView.systemUiVisibility = systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                decorView.systemUiVisibility = systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
        }
    }

    fun setLightNavigationbar(activity: Activity, enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val decorView = activity.window.decorView
            var systemUiVisibility = decorView.systemUiVisibility
            if (enabled) {
                systemUiVisibility = systemUiVisibility or SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                systemUiVisibility = systemUiVisibility and SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            }
            decorView.systemUiVisibility = systemUiVisibility
        }
    }

    fun setLightNavigationbarAuto(activity: Activity, bgColor: Int) {
        setLightNavigationbar(activity, ColorUtils.isColorLight(bgColor))
    }

    fun setNavigationbarColorAuto(activity: Activity) {
        setNavigationbarColor(activity, ThemeStore.navigationBarColor(activity))
    }

    fun setNavigationbarColor(activity: Activity, color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.window.navigationBarColor = color
            setLightNavigationbarAuto(activity, color)
        }
    }

    fun setTaskDescriptionColorAuto(activity: Activity) {
        setTaskDescriptionColor(activity, ThemeStore.primaryColor(activity))
    }

    fun setTaskDescriptionColor(activity: Activity, @ColorInt color: Int) {
        val color1: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Task description requires fully opaque color
            color1 = ColorUtils.stripAlpha(color)
            // Sets color of entry in the system recents page
            activity.setTaskDescription(ActivityManager.TaskDescription(activity.title as String, null, color1))
        }
    }

    fun setTint(view: View, @ColorInt color: Int) {
        TintHelper.setTintAuto(view, color, false)
    }

    fun setBackgroundTint(view: View, @ColorInt color: Int) {
        TintHelper.setTintAuto(view, color, true)
    }

    fun setAlertDialogTint(dialog: AlertDialog): AlertDialog {
        val colorStateList = Selector.colorBuild()
            .setDefaultColor(ThemeStore.accentColor(dialog.context))
            .setPressedColor(ColorUtils.darkenColor(ThemeStore.accentColor(dialog.context)))
            .create()
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(colorStateList)
        }
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(colorStateList)
        }
        return dialog
    }

    fun setEdgeEffectColor(view: RecyclerView, color: Int) {
        view.edgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
            override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
                val edgeEffect = super.createEdgeEffect(view, direction)
                edgeEffect.color = color
                return edgeEffect
            }
        }
    }
}