package me.ag2s.utils;

import android.os.Build;
import android.util.CloseGuard;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.R)
final class Android11CloseGuard implements AndroidCloseGuard {
    @NonNull
    private final CloseGuard mImpl;


    public Android11CloseGuard() {
        mImpl = new CloseGuard();
    }

    @Override
    public void open(@NonNull String closeMethodName) {
        mImpl.open(closeMethodName);
    }

    @Override
    public void close() {
        mImpl.close();
    }

    @Override
    public void warnIfOpen() {
        mImpl.warnIfOpen();
    }
}
