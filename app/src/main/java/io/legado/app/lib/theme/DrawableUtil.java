package io.legado.app.lib.theme;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import androidx.annotation.ColorInt;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public final class DrawableUtil {

    public static TransitionDrawable createTransitionDrawable(@ColorInt int startColor, @ColorInt int endColor) {
        return createTransitionDrawable(new ColorDrawable(startColor), new ColorDrawable(endColor));
    }

    public static TransitionDrawable createTransitionDrawable(Drawable start, Drawable end) {
        final Drawable[] drawables = new Drawable[2];

        drawables[0] = start;
        drawables[1] = end;

        return new TransitionDrawable(drawables);
    }

    private DrawableUtil() {
    }
}
