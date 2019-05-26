package io.legado.app.lib.theme;

import android.graphics.Color;
import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;

@SuppressWarnings({"unused", "WeakerAccess"})
public class ColorUtil {

    public static String intToString(int intColor) {
        return String.format("#%06X", 0xFFFFFF & intColor);
    }


    public static int stripAlpha(@ColorInt int color) {
        return 0xff000000 | color;
    }

    @ColorInt
    public static int shiftColor(@ColorInt int color, @FloatRange(from = 0.0f, to = 2.0f) float by) {
        if (by == 1f) return color;
        int alpha = Color.alpha(color);
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= by; // value component
        return (alpha << 24) + (0x00ffffff & Color.HSVToColor(hsv));
    }

    @ColorInt
    public static int darkenColor(@ColorInt int color) {
        return shiftColor(color, 0.9f);
    }

    @ColorInt
    public static int lightenColor(@ColorInt int color) {
        return shiftColor(color, 1.1f);
    }

    public static boolean isColorLight(@ColorInt int color) {
        final double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness < 0.4;
    }

    @ColorInt
    public static int invertColor(@ColorInt int color) {
        final int r = 255 - Color.red(color);
        final int g = 255 - Color.green(color);
        final int b = 255 - Color.blue(color);
        return Color.argb(Color.alpha(color), r, g, b);
    }

    @ColorInt
    public static int adjustAlpha(@ColorInt int color, @FloatRange(from = 0.0, to = 1.0) float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    @ColorInt
    public static int withAlpha(@ColorInt int baseColor, @FloatRange(from = 0.0, to = 1.0) float alpha) {
        int a = Math.min(255, Math.max(0, (int) (alpha * 255))) << 24;
        int rgb = 0x00ffffff & baseColor;
        return a + rgb;
    }

    /**
     * Taken from CollapsingToolbarLayout's CollapsingTextHelper class.
     */
    public static int blendColors(int color1, int color2, @FloatRange(from = 0.0, to = 1.0) float ratio) {
        final float inverseRatio = 1f - ratio;
        float a = (Color.alpha(color1) * inverseRatio) + (Color.alpha(color2) * ratio);
        float r = (Color.red(color1) * inverseRatio) + (Color.red(color2) * ratio);
        float g = (Color.green(color1) * inverseRatio) + (Color.green(color2) * ratio);
        float b = (Color.blue(color1) * inverseRatio) + (Color.blue(color2) * ratio);
        return Color.argb((int) a, (int) r, (int) g, (int) b);
    }

}
