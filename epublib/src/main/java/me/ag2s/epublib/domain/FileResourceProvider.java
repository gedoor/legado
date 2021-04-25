package me.ag2s.epublib.domain;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 用于创建epub，添加大文件（如大量图片）时容易OOM，使用LazyResource，避免OOM.
 *
 */

public class FileResourceProvider implements LazyResourceProvider {
    //需要导入资源的父目录
    String dir;

    /**
     * 创建一个文件夹里面文件夹的LazyResourceProvider，用于LazyResource。
     * @param dir 文件的目录
     */
    public FileResourceProvider(String dir) {
        this.dir = dir;
    }

    /**
     * 创建一个文件夹里面文件夹的LazyResourceProvider，用于LazyResource。
     * @param dirfile 文件夹
     */
    @SuppressWarnings("unused")
    public FileResourceProvider(File dirfile) {
        this.dir = dirfile.getPath();
    }

    @Override
    public InputStream getResourceStream(String href) throws IOException {
        return new FileInputStream(new File(dir, href));
    }
}
