package io.legado.app.ui.filechooser.utils;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;

/**
 * 存储设备工具类
 * <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
 * <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
 *
 * @author 李玉江[QQ:1023694760]
 * @since 2013-11-2
 */
public final class StorageUtils {

    /**
     * 判断外置存储是否可用
     *
     * @return the boolean
     */
    public static boolean externalMounted() {
        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            return true;
        }
        return false;
    }

    /**
     * 返回以“/”结尾的内部存储根目录
     */
    public static String getInternalRootPath(Context context, String type) {
        File file;
        if (TextUtils.isEmpty(type)) {
            file = context.getFilesDir();
        } else {
            file = new File(FileUtils.INSTANCE.separator(context.getFilesDir().getAbsolutePath()) + type);
            //noinspection ResultOfMethodCallIgnored
            file.mkdirs();
        }
        String path = "";
        if (file != null) {
            path = FileUtils.INSTANCE.separator(file.getAbsolutePath());
        }
        return path;
    }

    public static String getInternalRootPath(Context context) {
        return getInternalRootPath(context, null);
    }

    /**
     * 返回以“/”结尾的外部存储根目录，外置卡不可用则返回空字符串
     */
    public static String getExternalRootPath(String type) {
        File file = null;
        if (externalMounted()) {
            file = Environment.getExternalStorageDirectory();
        }
        if (file != null && !TextUtils.isEmpty(type)) {
            file = new File(file, type);
            //noinspection ResultOfMethodCallIgnored
            file.mkdirs();
        }
        String path = "";
        if (file != null) {
            path = FileUtils.INSTANCE.separator(file.getAbsolutePath());
        }
        return path;
    }

    public static String getExternalRootPath() {
        return getExternalRootPath(null);
    }

    /**
     * 各种类型的文件的专用的保存路径，以“/”结尾
     *
     * @return 诸如：/mnt/sdcard/Android/data/[package]/files/[type]/
     */
    public static String getExternalPrivatePath(Context context, String type) {
        File file = null;
        if (externalMounted()) {
            file = context.getExternalFilesDir(type);
        }
        //高频触发java.lang.NullPointerException，是SD卡不可用或暂时繁忙么？
        String path = "";
        if (file != null) {
            path = FileUtils.INSTANCE.separator(file.getAbsolutePath());
        }
        return path;
    }

    public static String getExternalPrivatePath(Context context) {
        return getExternalPrivatePath(context, null);
    }

    /**
     * 下载的文件的保存路径，必须为外部存储，以“/”结尾
     *
     * @return 诸如 ：/mnt/sdcard/Download/
     */
    public static String getDownloadPath() throws RuntimeException {
        File file;
        if (externalMounted()) {
            file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        } else {
            throw new RuntimeException("外置存储不可用！");
        }
        return FileUtils.INSTANCE.separator(file.getAbsolutePath());
    }

    /**
     * 各种类型的文件的专用的缓存存储保存路径，优先使用外置存储，以“/”结尾
     */
    public static String getCachePath(Context context, String type) {
        File file;
        if (externalMounted()) {
            file = context.getExternalCacheDir();
        } else {
            file = context.getCacheDir();
        }
        if (!TextUtils.isEmpty(type)) {
            file = new File(file, type);
            //noinspection ResultOfMethodCallIgnored
            file.mkdirs();
        }
        String path = "";
        if (file != null) {
            path = FileUtils.INSTANCE.separator(file.getAbsolutePath());
        }
        return path;
    }

    public static String getCachePath(Context context) {
        return getCachePath(context, null);
    }

    /**
     * 返回以“/”结尾的临时存储目录
     */
    public static String getTempDirPath(Context context) {
        return getExternalPrivatePath(context, "temporary");
    }

    /**
     * 返回临时存储文件路径
     */
    public static String getTempFilePath(Context context) {
        try {
            return File.createTempFile("lyj_", ".tmp", context.getCacheDir()).getAbsolutePath();
        } catch (IOException e) {
            return getTempDirPath(context) + "lyj.tmp";
        }
    }

}
