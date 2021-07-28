package io.legado.app.help.http.cronet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import org.chromium.net.CronetEngine;
import org.chromium.net.impl.ImplVersion;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CronetLoader extends CronetEngine.Builder.LibraryLoader {
    //https://storage.googleapis.com/chromium-cronet/android/92.0.4515.127/Release/cronet/libs/arm64-v8a/libcronet.92.0.4515.127.so
    //https://cdn.jsdelivr.net/gh/ag2s20150909/cronet-repo@92.0.4515.127/cronet/92.0.4515.127/arm64-v8a/libcronet.92.0.4515.127.so.js
    private final String soName = "libcronet." + ImplVersion.getCronetVersion() + ".so";
    private final String soUrl = "https://storage.googleapis.com/chromium-cronet/android/" + ImplVersion.getCronetVersion() + "/Release/cronet/libs/" + getCpuAbi() + "/" + soName;
    private final String md5Url = "https://cdn.jsdelivr.net/gh/ag2s20150909/cronet-repo@" + ImplVersion.getCronetVersion() + "/cronet/" + ImplVersion.getCronetVersion() + "/" + getCpuAbi() + "/" + soName + ".js";
    private final File soFile;
    private final File downloadFile;
    private static final String TAG = "CronetLoader";

    private static CronetLoader instance;

    public static CronetLoader getInstance(Context context) {
        if (mContext == null) {
            mContext = context;
        }
        if (instance == null) {
            synchronized (CronetLoader.class) {
                if (instance == null) {
                    instance = new CronetLoader(mContext);
                }
            }
        }
        return instance;
    }

    private static Context mContext;

    CronetLoader(Context context) {
        mContext = context.getApplicationContext();
        File dir = mContext.getDir("lib", Context.MODE_PRIVATE);
        soFile = new File(dir + "/" + getCpuAbi(), soName);
        downloadFile = new File(mContext.getCacheDir() + "/so_download", soName);


    }

    public boolean install() {
        return soFile.exists();
    }

    public void preDownload() {
        new Thread(() -> {
            String md5 = getUrlMd5(md5Url);
            if(soFile.exists()&&Objects.equals(md5, getFileMD5(soFile))){
                Log.e(TAG,"So 库已存在");
            }else {
                soFile.deleteOnExit();
                download(soUrl, md5, downloadFile, soFile);
            }

            Log.e(TAG, soName);
        }).start();

    }

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    @Override
    public void loadLibrary(String libName) {
        Log.e(TAG, "libName:" + libName);
        long start = System.currentTimeMillis();
        try {
            //非cronet的so调用系统方法加载
            if (!libName.contains("cronet")) {
                System.loadLibrary(libName);
                return;
            }
            //以下逻辑为cronet加载，优先加载本地，否则从远程加载
            //首先调用系统行为进行加载
            System.loadLibrary(libName);
            Log.e(TAG, "load from system");

        } catch (Throwable e) {
            //如果找不到，则从远程下载


            //删除历史文件
            deleteHistoryFile(Objects.requireNonNull(soFile.getParentFile()), soFile);

            Log.e(TAG, "soUrl:" + soUrl);
            String md5 = getUrlMd5(md5Url);
            Log.e(TAG, "soMD5:" + md5);
            Log.e(TAG, "soName+:" + soName);
            Log.e(TAG, "destSuccessFile:" + soFile);
            Log.e(TAG, "tempFile:" + downloadFile);


            if (md5 == null || md5.length() != 32 || soUrl.length() == 0) {
                //如果md5或下载的url为空，则调用系统行为进行加载
                System.loadLibrary(libName);
                return;
            }


            if (!soFile.exists() || !soFile.isFile()) {
                //noinspection ResultOfMethodCallIgnored
                soFile.delete();
                download(soUrl, md5, downloadFile, soFile);
                //如果文件不存在或不是文件，则调用系统行为进行加载
                System.loadLibrary(libName);
                return;
            }

            if (soFile.exists()) {
                //如果文件存在，则校验md5值
                String fileMD5 = getFileMD5(soFile);
                if (fileMD5 != null && fileMD5.equalsIgnoreCase(md5)) {
                    //md5值一样，则加载
                    System.load(soFile.getAbsolutePath());
                    Log.e(TAG, "load from:" + soFile);
                    return;
                }
                //md5不一样则删除
                //noinspection ResultOfMethodCallIgnored
                soFile.delete();

            }
            //不存在则下载
            download(soUrl, md5, downloadFile, soFile);
            //使用系统加载方法
            System.loadLibrary(libName);
        } finally {
            Log.e(TAG, "time:" + (System.currentTimeMillis() - start));
        }
    }


    public static String getCpuAbi() {
        //貌似只有这个过时了的API能获取当前APP使用的ABI
        return Build.CPU_ABI;
    }


    private static String getUrlMd5(String url) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            outputStream = new ByteArrayOutputStream();
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            inputStream = connection.getInputStream();
            byte[] buffer = new byte[32768];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
                outputStream.flush();
            }
            return outputStream.toString();

        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 删除历史文件
     */
    private static void deleteHistoryFile(File dir, File currentFile) {
        File[] files = dir.listFiles();
        if (files != null && files.length > 0) {
            for (File f : files) {
                if (f.exists() && (currentFile == null || !f.getAbsolutePath().equals(currentFile.getAbsolutePath()))) {
                    boolean delete = f.delete();
                    Log.e(TAG, "delete file: " + f + " result: " + delete);
                    if (!delete) {
                        f.deleteOnExit();
                    }
                }
            }
        }
    }

    /**
     * 下载文件
     */
    private static boolean downloadFileIfNotExist(String url, File destFile) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            inputStream = connection.getInputStream();
            if (destFile.exists()) {
                return true;
            }
            destFile.getParentFile().mkdirs();
            destFile.createNewFile();
            outputStream = new FileOutputStream(destFile);
            byte[] buffer = new byte[32768];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
                outputStream.flush();
            }
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
            if (destFile.exists() && !destFile.delete()) {
                destFile.deleteOnExit();
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    static boolean download = false;
    static Executor executor = Executors.newSingleThreadExecutor();

    /**
     * 下载并拷贝文件
     */
    private static synchronized void download(final String url, final String md5, final File downloadTempFile, final File destSuccessFile) {
        if (download) {
            return;
        }
        download = true;
        executor.execute(() -> {
            boolean result = downloadFileIfNotExist(url, downloadTempFile);
            Log.e(TAG, "download result:" + result);
            //文件md5再次校验
            String fileMD5 = getFileMD5(downloadTempFile);
            if (md5 != null && !md5.equalsIgnoreCase(fileMD5)) {
                boolean delete = downloadTempFile.delete();
                if (!delete) {
                    downloadTempFile.deleteOnExit();
                }
                download = false;
                return;
            }
            Log.e(TAG, "download success, copy to " + destSuccessFile);
            //下载成功拷贝文件
            copyFile(downloadTempFile, destSuccessFile);
            File parentFile = downloadTempFile.getParentFile();
            deleteHistoryFile(parentFile, null);
        });

    }


    /**
     * 拷贝文件
     */
    private static boolean copyFile(File source, File dest) {
        if (source == null || !source.exists() || !source.isFile() || dest == null) {
            return false;
        }
        if (source.getAbsolutePath().equals(dest.getAbsolutePath())) {
            return true;
        }
        FileInputStream is = null;
        FileOutputStream os = null;
        File parent = dest.getParentFile();
        if (parent != null && (!parent.exists())) {
            boolean mkdirs = parent.mkdirs();
            if (!mkdirs) {
                mkdirs = parent.mkdirs();
            }
        }
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest, false);

            byte[] buffer = new byte[1024 * 512];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * 获得文件md5
     */
    private static String getFileMD5(File file) {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[1024];
            int numRead = 0;
            while ((numRead = fileInputStream.read(buffer)) > 0) {
                md5.update(buffer, 0, numRead);
            }
            return String.format("%032x", new BigInteger(1, md5.digest())).toLowerCase();
        } catch (Exception e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
