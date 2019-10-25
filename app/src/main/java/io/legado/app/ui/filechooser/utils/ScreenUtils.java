package io.legado.app.ui.filechooser.utils;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Window;
import android.view.WindowManager;

/**
 * 获取屏幕宽高等信息、全屏切换、保持屏幕常亮、截屏等
 *
 * @author liyujiang[QQ:1032694760]
 * @since 2015/11/26
 */
public final class ScreenUtils {
    private static boolean isFullScreen = false;
    private static DisplayMetrics dm = null;

    public static DisplayMetrics displayMetrics(Context context) {
        if (null != dm) {
            return dm;
        }
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(dm);
        return dm;
    }

    public static int widthPixels(Context context) {
        return displayMetrics(context).widthPixels;
    }

    public static int heightPixels(Context context) {
        return displayMetrics(context).heightPixels;
    }

    public static float density(Context context) {
        return displayMetrics(context).density;
    }

    public static int densityDpi(Context context) {
        return displayMetrics(context).densityDpi;
    }

    public static boolean isFullScreen() {
        return isFullScreen;
    }

    public static void toggleFullScreen(Activity activity) {
        Window window = activity.getWindow();
        int flagFullscreen = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        if (isFullScreen) {
            window.clearFlags(flagFullscreen);
            isFullScreen = false;
        } else {
            window.setFlags(flagFullscreen, flagFullscreen);
            isFullScreen = true;
        }
    }

    /**
     * 保持屏幕常亮
     */
    public static void keepBright(Activity activity) {
        //需在setContentView前调用
        int keepScreenOn = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        activity.getWindow().setFlags(keepScreenOn, keepScreenOn);
    }

}
