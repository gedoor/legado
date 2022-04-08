package me.ag2s.utils;

import androidx.annotation.NonNull;

import java.io.IOException;

public class ThrowableUtils {


    public static @NonNull
    IOException rethrowAsIOException(Throwable throwable) throws IOException {
        IOException newException = new IOException(throwable.getMessage());
        newException.initCause(throwable);
        throw newException;
    }
}
