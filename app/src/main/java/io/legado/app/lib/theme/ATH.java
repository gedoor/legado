package io.legado.app.lib.theme;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.view.View;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import static android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public final class ATH {

    @SuppressLint("CommitPrefEdits")
    public static boolean didThemeValuesChange(@NonNull Context context, long since) {
        return ThemeStore.isConfigured(context) && ThemeStore.prefs(context).getLong(ThemeStore.VALUES_CHANGED, -1) > since;
    }

    public static void setStatusbarColorAuto(Activity activity) {
        setStatusbarColor(activity, ThemeStore.statusBarColor(activity));
    }

    public static void setStatusbarColor(Activity activity, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().setStatusBarColor(color);
            setLightStatusbarAuto(activity, color);
        }
    }

    public static void setLightStatusbarAuto(Activity activity, int bgColor) {
        setLightStatusbar(activity, ColorUtil.isColorLight(bgColor));
    }

    public static void setLightStatusbar(Activity activity, boolean enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final View decorView = activity.getWindow().getDecorView();
            final int systemUiVisibility = decorView.getSystemUiVisibility();
            if (enabled) {
                decorView.setSystemUiVisibility(systemUiVisibility | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                decorView.setSystemUiVisibility(systemUiVisibility & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }

    public static void setLightNavigationbar(Activity activity, boolean enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final View decorView = activity.getWindow().getDecorView();
            int systemUiVisibility = decorView.getSystemUiVisibility();
            if (enabled) {
                systemUiVisibility |= SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            } else {
                systemUiVisibility &= ~SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            decorView.setSystemUiVisibility(systemUiVisibility);
        }
    }

    public static void setLightNavigationbarAuto(Activity activity, int bgColor) {
        setLightNavigationbar(activity, ColorUtil.isColorLight(bgColor));
    }

    public static void setNavigationbarColorAuto(Activity activity) {
        setNavigationbarColor(activity, ThemeStore.navigationBarColor(activity));
    }

    public static void setNavigationbarColor(Activity activity, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().setNavigationBarColor(color);
            setLightNavigationbarAuto(activity, color);
        }
    }

    public static void setTaskDescriptionColorAuto(@NonNull Activity activity) {
        setTaskDescriptionColor(activity, ThemeStore.primaryColor(activity));
    }

    public static void setTaskDescriptionColor(@NonNull Activity activity, @ColorInt int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Task description requires fully opaque color
            color = ColorUtil.stripAlpha(color);
            // Sets color of entry in the system recents page
            activity.setTaskDescription(new ActivityManager.TaskDescription((String) activity.getTitle(), null, color));
        }
    }

    public static void setTint(@NonNull View view, @ColorInt int color) {
        TintHelper.setTintAuto(view, color, false);
    }

    public static void setBackgroundTint(@NonNull View view, @ColorInt int color) {
        TintHelper.setTintAuto(view, color, true);
    }

    public static AlertDialog setAlertDialogTint(@NonNull AlertDialog dialog) {
        ColorStateList colorStateList = Selector.colorBuild()
                .setDefaultColor(ThemeStore.accentColor(dialog.getContext()))
                .setPressedColor(ColorUtil.darkenColor(ThemeStore.accentColor(dialog.getContext())))
                .create();
        if (dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(colorStateList);
        }
        if (dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(colorStateList);
        }
        return dialog;
    }

    private ATH() {
    }
}