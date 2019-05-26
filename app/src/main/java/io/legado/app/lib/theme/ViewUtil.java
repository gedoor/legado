package io.legado.app.lib.theme;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.view.View;
import android.view.ViewTreeObserver;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public final class ViewUtil {

    @SuppressWarnings("deprecation")
    public static void removeOnGlobalLayoutListener(View v, ViewTreeObserver.OnGlobalLayoutListener listener) {
        v.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
    }

    @SuppressWarnings("deprecation")
    public static void setBackgroundCompat(@NonNull View view, @Nullable Drawable drawable) {
        view.setBackground(drawable);
    }

    public static TransitionDrawable setBackgroundTransition(@NonNull View view, @NonNull Drawable newDrawable) {
        TransitionDrawable transition = DrawableUtil.createTransitionDrawable(view.getBackground(), newDrawable);
        setBackgroundCompat(view, transition);
        return transition;
    }

    public static TransitionDrawable setBackgroundColorTransition(@NonNull View view, @ColorInt int newColor) {
        final Drawable oldColor = view.getBackground();

        Drawable start = oldColor != null ? oldColor : new ColorDrawable(view.getSolidColor());
        Drawable end = new ColorDrawable(newColor);

        TransitionDrawable transition = DrawableUtil.createTransitionDrawable(start, end);

        setBackgroundCompat(view, transition);

        return transition;
    }

    private ViewUtil() {
    }
}
