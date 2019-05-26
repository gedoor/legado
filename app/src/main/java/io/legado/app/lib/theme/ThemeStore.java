package io.legado.app.lib.theme;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import androidx.annotation.*;
import androidx.core.content.ContextCompat;
import io.legado.app.R;

/**
 * @author Aidan Follestad (afollestad), Karim Abou Zeid (kabouzeid)
 */
public final class ThemeStore implements ThemeStorePrefKeys, ThemeStoreInterface {

    private final Context mContext;
    private final SharedPreferences.Editor mEditor;

    public static ThemeStore editTheme(@NonNull Context context) {
        return new ThemeStore(context);
    }

    @SuppressLint("CommitPrefEdits")
    private ThemeStore(@NonNull Context context) {
        mContext = context;
        mEditor = prefs(context).edit();
    }


    @Override
    public ThemeStore primaryColor(@ColorInt int color) {
        mEditor.putInt(KEY_PRIMARY_COLOR, color);
        if (autoGeneratePrimaryDark(mContext))
            primaryColorDark(ColorUtil.darkenColor(color));
        return this;
    }

    @Override
    public ThemeStore primaryColorRes(@ColorRes int colorRes) {
        return primaryColor(ContextCompat.getColor(mContext, colorRes));
    }

    @Override
    public ThemeStore primaryColorAttr(@AttrRes int colorAttr) {
        return primaryColor(ATHUtil.resolveColor(mContext, colorAttr));
    }

    @Override
    public ThemeStore primaryColorDark(@ColorInt int color) {
        mEditor.putInt(KEY_PRIMARY_COLOR_DARK, color);
        return this;
    }

    @Override
    public ThemeStore primaryColorDarkRes(@ColorRes int colorRes) {
        return primaryColorDark(ContextCompat.getColor(mContext, colorRes));
    }

    @Override
    public ThemeStore primaryColorDarkAttr(@AttrRes int colorAttr) {
        return primaryColorDark(ATHUtil.resolveColor(mContext, colorAttr));
    }

    @Override
    public ThemeStore accentColor(@ColorInt int color) {
        mEditor.putInt(KEY_ACCENT_COLOR, color);
        return this;
    }

    @Override
    public ThemeStore accentColorRes(@ColorRes int colorRes) {
        return accentColor(ContextCompat.getColor(mContext, colorRes));
    }

    @Override
    public ThemeStore accentColorAttr(@AttrRes int colorAttr) {
        return accentColor(ATHUtil.resolveColor(mContext, colorAttr));
    }

    @Override
    public ThemeStore statusBarColor(@ColorInt int color) {
        mEditor.putInt(KEY_STATUS_BAR_COLOR, color);
        return this;
    }

    @Override
    public ThemeStore statusBarColorRes(@ColorRes int colorRes) {
        return statusBarColor(ContextCompat.getColor(mContext, colorRes));
    }

    @Override
    public ThemeStore statusBarColorAttr(@AttrRes int colorAttr) {
        return statusBarColor(ATHUtil.resolveColor(mContext, colorAttr));
    }

    @Override
    public ThemeStore navigationBarColor(@ColorInt int color) {
        mEditor.putInt(KEY_NAVIGATION_BAR_COLOR, color);
        return this;
    }

    @Override
    public ThemeStore navigationBarColorRes(@ColorRes int colorRes) {
        return navigationBarColor(ContextCompat.getColor(mContext, colorRes));
    }

    @Override
    public ThemeStore navigationBarColorAttr(@AttrRes int colorAttr) {
        return navigationBarColor(ATHUtil.resolveColor(mContext, colorAttr));
    }

    @Override
    public ThemeStore textColorPrimary(@ColorInt int color) {
        mEditor.putInt(KEY_TEXT_COLOR_PRIMARY, color);
        return this;
    }

    @Override
    public ThemeStore textColorPrimaryRes(@ColorRes int colorRes) {
        return textColorPrimary(ContextCompat.getColor(mContext, colorRes));
    }

    @Override
    public ThemeStore textColorPrimaryAttr(@AttrRes int colorAttr) {
        return textColorPrimary(ATHUtil.resolveColor(mContext, colorAttr));
    }

    @Override
    public ThemeStore textColorPrimaryInverse(@ColorInt int color) {
        mEditor.putInt(KEY_TEXT_COLOR_PRIMARY_INVERSE, color);
        return this;
    }

    @Override
    public ThemeStore textColorPrimaryInverseRes(@ColorRes int colorRes) {
        return textColorPrimaryInverse(ContextCompat.getColor(mContext, colorRes));
    }

    @Override
    public ThemeStore textColorPrimaryInverseAttr(@AttrRes int colorAttr) {
        return textColorPrimaryInverse(ATHUtil.resolveColor(mContext, colorAttr));
    }

    @Override
    public ThemeStore textColorSecondary(@ColorInt int color) {
        mEditor.putInt(KEY_TEXT_COLOR_SECONDARY, color);
        return this;
    }

    @Override
    public ThemeStore textColorSecondaryRes(@ColorRes int colorRes) {
        return textColorSecondary(ContextCompat.getColor(mContext, colorRes));
    }

    @Override
    public ThemeStore textColorSecondaryAttr(@AttrRes int colorAttr) {
        return textColorSecondary(ATHUtil.resolveColor(mContext, colorAttr));
    }

    @Override
    public ThemeStore textColorSecondaryInverse(@ColorInt int color) {
        mEditor.putInt(KEY_TEXT_COLOR_SECONDARY_INVERSE, color);
        return this;
    }

    @Override
    public ThemeStore textColorSecondaryInverseRes(@ColorRes int colorRes) {
        return textColorSecondaryInverse(ContextCompat.getColor(mContext, colorRes));
    }

    @Override
    public ThemeStore textColorSecondaryInverseAttr(@AttrRes int colorAttr) {
        return textColorSecondaryInverse(ATHUtil.resolveColor(mContext, colorAttr));
    }

    @Override
    public ThemeStore backgroundColor(int color) {
        mEditor.putInt(KEY_BACKGROUND_COLOR, color);
        return this;
    }

    @Override
    public ThemeStore coloredStatusBar(boolean colored) {
        mEditor.putBoolean(KEY_APPLY_PRIMARYDARK_STATUSBAR, colored);
        return this;
    }

    @Override
    public ThemeStore coloredNavigationBar(boolean applyToNavBar) {
        mEditor.putBoolean(KEY_APPLY_PRIMARY_NAVBAR, applyToNavBar);
        return this;
    }

    @Override
    public ThemeStore autoGeneratePrimaryDark(boolean autoGenerate) {
        mEditor.putBoolean(KEY_AUTO_GENERATE_PRIMARYDARK, autoGenerate);
        return this;
    }

    // Commit method

    @SuppressWarnings("unchecked")
    @Override
    public void apply() {
        mEditor.putLong(VALUES_CHANGED, System.currentTimeMillis())
                .putBoolean(IS_CONFIGURED_KEY, true)
                .apply();
    }

    // Static getters

    @CheckResult
    @NonNull
    protected static SharedPreferences prefs(@NonNull Context context) {
        return context.getSharedPreferences(CONFIG_PREFS_KEY_DEFAULT, Context.MODE_PRIVATE);
    }

    public static void markChanged(@NonNull Context context) {
        new ThemeStore(context).apply();
    }

    @CheckResult
    @ColorInt
    public static int primaryColor(@NonNull Context context) {
        return prefs(context).getInt(KEY_PRIMARY_COLOR, ATHUtil.resolveColor(context, R.attr.colorPrimary, Color.parseColor("#455A64")));
    }

    @CheckResult
    @ColorInt
    public static int primaryColorDark(@NonNull Context context) {
        return prefs(context).getInt(KEY_PRIMARY_COLOR_DARK, ATHUtil.resolveColor(context, R.attr.colorPrimaryDark, Color.parseColor("#37474F")));
    }

    @CheckResult
    @ColorInt
    public static int accentColor(@NonNull Context context) {
        return prefs(context).getInt(KEY_ACCENT_COLOR, ATHUtil.resolveColor(context, R.attr.colorAccent, Color.parseColor("#263238")));
    }

    @CheckResult
    @ColorInt
    public static int statusBarColor(@NonNull Context context) {
        if (!coloredStatusBar(context)) {
            return Color.BLACK;
        }
        return prefs(context).getInt(KEY_STATUS_BAR_COLOR, primaryColorDark(context));
    }

    @CheckResult
    @ColorInt
    public static int navigationBarColor(@NonNull Context context) {
        if (!coloredNavigationBar(context)) {
            return Color.BLACK;
        }
        return prefs(context).getInt(KEY_NAVIGATION_BAR_COLOR, primaryColor(context));
    }

    @CheckResult
    @ColorInt
    public static int textColorPrimary(@NonNull Context context) {
        return prefs(context).getInt(KEY_TEXT_COLOR_PRIMARY, ATHUtil.resolveColor(context, android.R.attr.textColorPrimary));
    }

    @CheckResult
    @ColorInt
    public static int textColorPrimaryInverse(@NonNull Context context) {
        return prefs(context).getInt(KEY_TEXT_COLOR_PRIMARY_INVERSE, ATHUtil.resolveColor(context, android.R.attr.textColorPrimaryInverse));
    }

    @CheckResult
    @ColorInt
    public static int textColorSecondary(@NonNull Context context) {
        return prefs(context).getInt(KEY_TEXT_COLOR_SECONDARY, ATHUtil.resolveColor(context, android.R.attr.textColorSecondary));
    }

    @CheckResult
    @ColorInt
    public static int textColorSecondaryInverse(@NonNull Context context) {
        return prefs(context).getInt(KEY_TEXT_COLOR_SECONDARY_INVERSE, ATHUtil.resolveColor(context, android.R.attr.textColorSecondaryInverse));
    }

    @CheckResult
    @ColorInt
    public static int backgroundColor(@NonNull Context context) {
        return prefs(context).getInt(KEY_BACKGROUND_COLOR, ATHUtil.resolveColor(context, android.R.attr.colorBackground));
    }

    @CheckResult
    public static boolean coloredStatusBar(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_APPLY_PRIMARYDARK_STATUSBAR, true);
    }

    @CheckResult
    public static boolean coloredNavigationBar(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_APPLY_PRIMARY_NAVBAR, false);
    }

    @CheckResult
    public static boolean autoGeneratePrimaryDark(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_AUTO_GENERATE_PRIMARYDARK, true);
    }

    @CheckResult
    public static boolean isConfigured(Context context) {
        return prefs(context).getBoolean(IS_CONFIGURED_KEY, false);
    }

    @SuppressLint("CommitPrefEdits")
    public static boolean isConfigured(Context context, @IntRange(from = 0, to = Integer.MAX_VALUE) int version) {
        final SharedPreferences prefs = prefs(context);
        final int lastVersion = prefs.getInt(IS_CONFIGURED_VERSION_KEY, -1);
        if (version > lastVersion) {
            prefs.edit().putInt(IS_CONFIGURED_VERSION_KEY, version).apply();
            return false;
        }
        return true;
    }
}