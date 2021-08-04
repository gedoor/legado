package me.ag2s.umdlib.umd;

import java.io.IOException;
import java.io.InputStream;


import me.ag2s.umdlib.domain.UmdBook;
import me.ag2s.umdlib.domain.UmdCover;
import me.ag2s.umdlib.domain.UmdHeader;
import me.ag2s.umdlib.tool.StreamReader;
import me.ag2s.umdlib.tool.UmdUtils;

/**
 * UMD格式的电子书解析
 * 格式规范参考：
 * http://blog.sina.com.cn/s/blog_7c8dc2d501018o5d.html
 * http://blog.sina.com.cn/s/blog_7c8dc2d501018o5l.html
 *
 */

public class UmdReader {
    UmdBook book;
    InputStream inputStream;
    int _AdditionalCheckNumber;
    int _TotalContentLen;
    boolean end = false;


    public synchronized UmdBook read(InputStream inputStream) throws Exception {

        book = new UmdBook();
        this.inputStream=inputStream;
        StreamReader reader = new StreamReader(inputStream);
        UmdHeader umdHeader = new UmdHeader();
        book.setHeader(umdHeader);
        if (reader.readIntLe() != 0xde9a9b89) {
            throw new IOException("Wrong header");
        }
        short num1 = -1;
        byte ch = reader.readByte();
        while (ch == 35) {
            //int num2=reader.readByte();
            short segType = reader.readShortLe();
            byte segFlag = reader.readByte();
            short len = (short) (reader.readUint8() - 5);

            System.out.println("块标识:" + segType);
            //short length1 = reader.readByte();
            ReadSection(segType, segFlag, len, reader, umdHeader);

            if ((int) segType == 241 || (int) segType == 10) {
                segType = num1;
            }
            for (ch = reader.readByte(); ch == 36; ch = reader.readByte()) {
                //int num3 = reader.readByte();
                System.out.println(ch);
                int additionalCheckNumber = reader.readIntLe();
                int length2 = (reader.readIntLe() - 9);
                ReadAdditionalSection(segType, additionalCheckNumber, length2, reader);
            }
            num1 = segType;

        }
        System.out.println(book.getHeader().toString());
        return book;

    }

    private void ReadAdditionalSection(short segType, int additionalCheckNumber, int length, StreamReader reader) throws Exception {
        switch (segType) {
            case 14:
                //this._TotalImageList.Add((object) Image.FromStream((Stream) new MemoryStream(reader.ReadBytes((int) length))));
                break;
            case 15:
                //this._TotalImageList.Add((object) Image.FromStream((Stream) new MemoryStream(reader.ReadBytes((int) length))));
                break;
            case 129:
                reader.readBytes(length);
                break;
            case 130:
                //byte[] covers = reader.readBytes(length);
                book.setCover(new UmdCover(reader.readBytes(length)));
                //this._Book.Cover = BitmapImage.FromStream((Stream) new MemoryStream(reader.ReadBytes((int) length)));
                break;
            case 131:
                System.out.println(length / 4);
                book.setNum(length / 4);
                for (int i = 0; i < length / 4; ++i) {
                    book.getChapters().addContentLength(reader.readIntLe());
                }
                break;
            case 132:
                //System.out.println(length/4);
                System.out.println(_AdditionalCheckNumber);
                System.out.println(additionalCheckNumber);
                if (this._AdditionalCheckNumber != additionalCheckNumber) {
                    System.out.println(length);
                    book.getChapters().contents.write(UmdUtils.decompress(reader.readBytes(length)));
                    book.getChapters().contents.flush();
                    break;
                } else {
                    for (int i = 0; i < book.getNum(); i++) {
                        short len = reader.readUint8();
                        byte[] title = reader.readBytes(len);
                        //System.out.println(UmdUtils.unicodeBytesToString(title));
                        book.getChapters().addTitle(title);
                    }
                }


                break;
            default:
                    /*Console.WriteLine("未知内容");
                    Console.WriteLine("Seg Type = " + (object) segType);
                    Console.WriteLine("Seg Len = " + (object) length);
                    Console.WriteLine("content = " + (object) reader.ReadBytes((int) length));*/
                break;
        }
    }

    public void ReadSection(short segType, byte segFlag, short length, StreamReader reader, UmdHeader header) throws IOException {
        switch (segType) {
            case 1://umd文件头 DCTS_CMD_ID_VERSION
                header.setUmdType(reader.readByte());
                reader.readBytes(2);//Random 2
                System.out.println("UMD文件类型:" + header.getUmdType());
                break;
            case 2://文件标题 DCTS_CMD_ID_TITLE
                header.setTitle(UmdUtils.unicodeBytesToString(reader.readBytes(length)));
                System.out.println("文件标题:" + header.getTitle());
                break;
            case 3://作者
                header.setAuthor(UmdUtils.unicodeBytesToString(reader.readBytes(length)));
                System.out.println("作者:" + header.getAuthor());
                break;
            case 4://年
                header.setYear(UmdUtils.unicodeBytesToString(reader.readBytes(length)));
                System.out.println("年:" + header.getYear());
                break;
            case 5://月
                header.setMonth(UmdUtils.unicodeBytesToString(reader.readBytes(length)));
                System.out.println("月:" + header.getMonth());
                break;
            case 6://日
                header.setDay(UmdUtils.unicodeBytesToString(reader.readBytes(length)));
                System.out.println("日:" + header.getDay());
                break;
            case 7://小说类型
                header.setBookType(UmdUtils.unicodeBytesToString(reader.readBytes(length)));
                System.out.println("小说类型:" + header.getBookType());
                break;
            case 8://出版商
                header.setBookMan(UmdUtils.unicodeBytesToString(reader.readBytes(length)));
                System.out.println("出版商:" + header.getBookMan());
                break;
            case 9:// 零售商
                header.setShopKeeper(UmdUtils.unicodeBytesToString(reader.readBytes(length)));
                System.out.println("零售商:" + header.getShopKeeper());
                break;
            case 10://CONTENT ID
                System.out.println("CONTENT ID:" + reader.readHex(length));
                break;
            case 11:
                //内容长度 DCTS_CMD_ID_FILE_LENGTH
                _TotalContentLen = reader.readIntLe();
                book.getChapters().setTotalContentLen(_TotalContentLen);
                System.out.println("内容长度:" + _TotalContentLen);
                break;
            case 12://UMD文件结束
                end = true;
                int num2 = reader.readIntLe();
                System.out.println("整个文件长度" + num2);
                break;
            case 13:
                break;
            case 14:
                int num3 = (int) reader.readByte();
                break;
            case 15:
                reader.readBytes(length);
                break;
            case 129://正文
            case 131://章节偏移
                _AdditionalCheckNumber = reader.readIntLe();
                System.out.println("章节偏移:" + _AdditionalCheckNumber);
                break;
            case 132://章节标题，正文
                _AdditionalCheckNumber = reader.readIntLe();
                System.out.println("章节标题，正文:" + _AdditionalCheckNumber);
                break;
            case 130://封面（jpg）
                int num4 = (int) reader.readByte();
                _AdditionalCheckNumber = reader.readIntLe();
                break;
            case 135://页面偏移（Page Offset）
                reader.readUint8();//fontSize 一字节 字体大小
                reader.readUint8();//screenWidth 屏幕宽度
                reader.readBytes(4);//BlockRandom 指向一个页面偏移数据块
                break;
            case 240://CDS KEY
                break;
            case 241://许可证(LICENCE KEY)
                //System.out.println("整个文件长度" + length);
                System.out.println("许可证(LICENCE KEY):" + reader.readHex(16));
                break;
            default:
                if (length > 0) {
                    byte[] numArray = reader.readBytes(length);
                }


        }
    }


    @Override
    public String toString() {
        return "UmdReader{" +
                "book=" + book +
                '}';
    }
}
