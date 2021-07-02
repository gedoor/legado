package me.ag2s.umdlib.domain;

import java.io.IOException;
import java.io.OutputStream;

import me.ag2s.umdlib.tool.WrapOutputStream;

public class UmdBook {

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    private int num;


    /** Header Part of UMD book */
    private UmdHeader header = new UmdHeader();
    /**
     * Detail chapters Part of UMD book
     * (include Titles & Contents of each chapter)
     */
    private UmdChapters chapters = new UmdChapters();

    /** Cover Part of UMD book (for example, and JPEG file) */
    private UmdCover cover = new UmdCover();

    /** End Part of UMD book */
    private UmdEnd end = new UmdEnd();

    /**
     * Build the UMD file.
     * @param os
     * @throws IOException
     */
    public void buildUmd(OutputStream os) throws IOException {
        WrapOutputStream wos = new WrapOutputStream(os);

        header.buildHeader(wos);
        chapters.buildChapters(wos);
        cover.buildCover(wos);
        end.buildEnd(wos);
    }

    public UmdHeader getHeader() {
        return header;
    }

    public void setHeader(UmdHeader header) {
        this.header = header;
    }

    public UmdChapters getChapters() {
        return chapters;
    }

    public void setChapters(UmdChapters chapters) {
        this.chapters = chapters;
    }

    public UmdCover getCover() {
    return cover;
    }

    public void setCover(UmdCover cover) {
    this.cover = cover;
    }

    public UmdEnd getEnd() {
        return end;
    }

    public void setEnd(UmdEnd end) {
        this.end = end;
    }

}
