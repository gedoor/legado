package me.ag2s.utils;


import android.os.Build;

import androidx.annotation.NonNull;

public interface AndroidCloseGuard {


    static AndroidCloseGuard getInstance() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return new Android11CloseGuard();
        } else {
            return new AndroidRefCloseGuard();
        }
    }

    /**
     * Initializes the instance with a warning that the caller should have explicitly called the
     * {@code closeMethodName} method instead of relying on finalization.
     *
     * @param closeMethodName non-null name of explicit termination method. Printed by warnIfOpen.
     * @throws NullPointerException if closeMethodName is null.
     */
    void open(@NonNull String closeMethodName);

    /**
     * Marks this CloseGuard instance as closed to avoid warnings on finalization.
     */
    void close();

    /**
     * Logs a warning if the caller did not properly cleanup by calling an explicit close method
     * before finalization.
     */
    void warnIfOpen();
}
