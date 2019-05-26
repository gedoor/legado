package io.legado.app.lib.theme;

import android.content.res.ColorStateList;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import com.google.android.material.internal.NavigationMenuView;
import com.google.android.material.navigation.NavigationView;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public final class NavigationViewUtil {

    public static void setItemIconColors(@NonNull NavigationView navigationView, @ColorInt int normalColor, @ColorInt int selectedColor) {
        final ColorStateList iconSl = new ColorStateList(
                new int[][]{
                        new int[]{-android.R.attr.state_checked},
                        new int[]{android.R.attr.state_checked}
                },
                new int[]{
                        normalColor,
                        selectedColor
                });
        navigationView.setItemIconTintList(iconSl);
    }

    public static void setItemTextColors(@NonNull NavigationView navigationView, @ColorInt int normalColor, @ColorInt int selectedColor) {
        final ColorStateList textSl = new ColorStateList(
                new int[][]{
                        new int[]{-android.R.attr.state_checked},
                        new int[]{android.R.attr.state_checked}
                },
                new int[]{
                        normalColor,
                        selectedColor
                });
        navigationView.setItemTextColor(textSl);
    }

    /**
     * 去掉navigationView的滚动条
     * @param navigationView NavigationView
     */
    public static void disableScrollbar(NavigationView navigationView) {
        if (navigationView != null) {
            NavigationMenuView navigationMenuView = (NavigationMenuView) navigationView.getChildAt(0);
            if (navigationMenuView != null) {
                navigationMenuView.setVerticalScrollBarEnabled(false);
            }
        }
    }

    private NavigationViewUtil() {
    }
}
