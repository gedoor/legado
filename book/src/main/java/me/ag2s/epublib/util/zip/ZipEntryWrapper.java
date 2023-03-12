package me.ag2s.epublib.util.zip;

import androidx.annotation.NonNull;

import java.util.zip.ZipEntry;

public class ZipEntryWrapper {
    @NonNull
    private final Object zipEntry;

    public void checkType() {

        if (zipEntry instanceof java.util.zip.ZipEntry || zipEntry instanceof AndroidZipEntry) {
        } else {
            throw new RuntimeException("使用了不支持的类");
        }
    }

    public ZipEntryWrapper(@NonNull ZipEntry zipEntry) {
        this.zipEntry = zipEntry;
    }

    public ZipEntryWrapper(@NonNull AndroidZipEntry zipEntry) {
        this.zipEntry = zipEntry;
    }

    public ZipEntryWrapper(@NonNull Object element) {

        this.zipEntry = element;
        checkType();
    }

    public boolean isDirectory() {
        checkType();
        if (zipEntry instanceof ZipEntry) {
            return ((ZipEntry) zipEntry).isDirectory();
        }
        if (zipEntry instanceof AndroidZipEntry) {
            return ((AndroidZipEntry) zipEntry).isDirectory();
        }
        return true;
    }

    public ZipEntry getZipEntry() {
        return (ZipEntry) zipEntry;
    }

    public AndroidZipEntry getAndroidZipEntry() {
        return (AndroidZipEntry) zipEntry;
    }

    public String getName() {
        checkType();
        if (zipEntry instanceof ZipEntry) {
            return ((ZipEntry) zipEntry).getName();
        }
        if (zipEntry instanceof AndroidZipEntry) {
            return ((AndroidZipEntry) zipEntry).getName();
        }
        return null;
    }

    public long getSize() {
        checkType();
        if (zipEntry instanceof ZipEntry) {
            return ((ZipEntry) zipEntry).getSize();
        }
        if (zipEntry instanceof AndroidZipEntry) {
            return ((AndroidZipEntry) zipEntry).getSize();
        }
        return -1;
    }


}
