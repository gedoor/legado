package me.ag2s.epublib.util;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;

final class AndroidRefCloseGuard implements AndroidCloseGuard {
    private static Object closeGuardInstance;
    private static Method getMethod;
    private static Method closeMethod;
    private static Method openMethod;
    private static Method warnIfOpenMethod;


    public AndroidRefCloseGuard() {

        if (getMethod == null || closeMethod == null || openMethod == null || warnIfOpenMethod == null) {
            try {
                Class<?> closeGuardClass = Class.forName("dalvik.system.CloseGuard");
                getMethod = closeGuardClass.getMethod("get");
                closeMethod = closeGuardClass.getMethod("close");
                openMethod = closeGuardClass.getMethod("open", String.class);
                warnIfOpenMethod = closeGuardClass.getMethod("warnIfOpen");
            } catch (Exception ignored) {
                getMethod = null;
                openMethod = null;
                warnIfOpenMethod = null;
            }
        }


    }

    Object createAndOpen(String closer) {
        if (getMethod != null) {
            try {
                if (closeGuardInstance == null) {
                    closeGuardInstance = getMethod.invoke(null);
                }

                openMethod.invoke(closeGuardInstance, closer);
                return closeGuardInstance;
            } catch (Exception ignored) {
            }
        }
        return null;
    }


    boolean warnIfOpen(Object closeGuardInstance) {
        boolean reported = false;
        if (closeGuardInstance != null) {
            try {
                warnIfOpenMethod.invoke(closeGuardInstance);
                reported = true;
            } catch (Exception ignored) {
            }
        }
        return reported;
    }


    @Override
    public void open(@NonNull String closeMethodName) {
        closeGuardInstance = createAndOpen(closeMethodName);
    }

    @Override
    public void close() {
        if (closeGuardInstance != null) {
            try {
                closeMethod.invoke(closeMethod);
            } catch (Exception ignored) {
            }
        }

    }

    @Override
    public void warnIfOpen() {
        warnIfOpen(closeGuardInstance);
    }
}
