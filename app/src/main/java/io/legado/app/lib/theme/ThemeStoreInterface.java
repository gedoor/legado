package io.legado.app.lib.theme;


import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;

/**
 * @author Aidan Follestad (afollestad), Karim Abou Zeid (kabouzeid)
 */
interface ThemeStoreInterface {

    // Primary colors

    ThemeStore primaryColor(@ColorInt int color);

    ThemeStore primaryColorRes(@ColorRes int colorRes);

    ThemeStore primaryColorAttr(@AttrRes int colorAttr);

    ThemeStore autoGeneratePrimaryDark(boolean autoGenerate);

    ThemeStore primaryColorDark(@ColorInt int color);

    ThemeStore primaryColorDarkRes(@ColorRes int colorRes);

    ThemeStore primaryColorDarkAttr(@AttrRes int colorAttr);

    // Accent colors

    ThemeStore accentColor(@ColorInt int color);

    ThemeStore accentColorRes(@ColorRes int colorRes);

    ThemeStore accentColorAttr(@AttrRes int colorAttr);

    // Status bar color

    ThemeStore statusBarColor(@ColorInt int color);

    ThemeStore statusBarColorRes(@ColorRes int colorRes);

    ThemeStore statusBarColorAttr(@AttrRes int colorAttr);

    // Navigation bar color

    ThemeStore navigationBarColor(@ColorInt int color);

    ThemeStore navigationBarColorRes(@ColorRes int colorRes);

    ThemeStore navigationBarColorAttr(@AttrRes int colorAttr);

    // Primary text color

    ThemeStore textColorPrimary(@ColorInt int color);

    ThemeStore textColorPrimaryRes(@ColorRes int colorRes);

    ThemeStore textColorPrimaryAttr(@AttrRes int colorAttr);

    ThemeStore textColorPrimaryInverse(@ColorInt int color);

    ThemeStore textColorPrimaryInverseRes(@ColorRes int colorRes);

    ThemeStore textColorPrimaryInverseAttr(@AttrRes int colorAttr);

    // Secondary text color

    ThemeStore textColorSecondary(@ColorInt int color);

    ThemeStore textColorSecondaryRes(@ColorRes int colorRes);

    ThemeStore textColorSecondaryAttr(@AttrRes int colorAttr);

    ThemeStore textColorSecondaryInverse(@ColorInt int color);

    ThemeStore textColorSecondaryInverseRes(@ColorRes int colorRes);

    ThemeStore textColorSecondaryInverseAttr(@AttrRes int colorAttr);

    ThemeStore backgroundColor(@ColorInt int color);

    // Toggle configurations

    ThemeStore coloredStatusBar(boolean colored);

    ThemeStore coloredNavigationBar(boolean applyToNavBar);

    // Commit/apply

    void apply();
}
