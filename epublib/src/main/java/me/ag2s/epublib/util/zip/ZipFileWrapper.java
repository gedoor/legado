package me.ag2s.epublib.util.zip;


import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipFile;

/**
 * 对ZipFile的包装
 */

public class ZipFileWrapper {
    @NonNull
    private final Object zipFile;


    public void checkType() {
        if (zipFile instanceof java.util.zip.ZipFile || zipFile instanceof AndroidZipFile) {
        } else {
            throw new RuntimeException("使用了不支持的类");
        }
    }

    public ZipFileWrapper(@NonNull ZipFile zipFile) {
        this.zipFile = zipFile;
        checkType();
    }

    public ZipFileWrapper(@NonNull AndroidZipFile zipFile) {
        this.zipFile = zipFile;
        checkType();
    }

    public String getName() {
        checkType();
        if (zipFile instanceof java.util.zip.ZipFile) {
            return ((ZipFile) zipFile).getName();
        } else if (zipFile instanceof AndroidZipFile) {
            return ((AndroidZipFile) zipFile).getName();
        } else {
            return null;
        }
    }

    public String getComment() {
        checkType();
        if (zipFile instanceof java.util.zip.ZipFile) {
            return ((ZipFile) zipFile).getComment();
        } else if (zipFile instanceof AndroidZipFile) {
            return ((AndroidZipFile) zipFile).getName();
        } else {
            return null;
        }
    }

    public ZipEntryWrapper getEntry(String name) {
        checkType();
        if (zipFile instanceof java.util.zip.ZipFile) {
            return new ZipEntryWrapper(((ZipFile) zipFile).getEntry(name));
        } else if (zipFile instanceof AndroidZipFile) {
            return new ZipEntryWrapper(((AndroidZipFile) zipFile).getEntry(name));
        } else {
            return null;
        }
    }

    public Enumeration entries() {
        checkType();
        if (zipFile instanceof java.util.zip.ZipFile) {
            return ((ZipFile) zipFile).entries();

        } else if (zipFile instanceof AndroidZipFile) {
            return ((AndroidZipFile) zipFile).entries();
        } else {
            return null;
        }
    }

    public InputStream getInputStream(ZipEntryWrapper entry) throws IOException {
        checkType();
        if (zipFile instanceof java.util.zip.ZipFile) {
            return ((ZipFile) zipFile).getInputStream(entry.getZipEntry());
        } else if (zipFile instanceof AndroidZipFile) {
            return ((AndroidZipFile) zipFile).getInputStream(entry.getAndroidZipEntry());
        } else {
            return null;
        }
    }

    public void close() throws IOException {
        checkType();
        if (zipFile instanceof java.util.zip.ZipFile) {
            ((ZipFile) zipFile).close();
        } else if (zipFile instanceof AndroidZipFile) {
            ((AndroidZipFile) zipFile).close();
        }
    }


}
