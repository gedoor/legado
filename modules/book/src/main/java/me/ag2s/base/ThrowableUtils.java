package me.ag2s.base;

import androidx.annotation.NonNull;

import java.io.IOException;

public class ThrowableUtils {


    public static @NonNull
    IOException rethrowAsIOException(Throwable throwable) throws IOException {
        IOException newException = new IOException(throwable.getMessage(), throwable);
        throw newException;
    }
}
