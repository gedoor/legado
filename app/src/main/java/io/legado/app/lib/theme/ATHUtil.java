package io.legado.app.lib.theme;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.AttrRes;

/**
 * @author Aidan Follestad (afollestad)
 */
public final class ATHUtil {

    public static boolean isWindowBackgroundDark(Context context) {
        return !ColorUtil.isColorLight(ATHUtil.resolveColor(context, android.R.attr.windowBackground));
    }

    public static int resolveColor(Context context, @AttrRes int attr) {
        return resolveColor(context, attr, 0);
    }

    public static int resolveColor(Context context, @AttrRes int attr, int fallback) {
        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{attr});
        try {
            return a.getColor(0, fallback);
        } finally {
            a.recycle();
        }
    }

    private ATHUtil() {
    }
}