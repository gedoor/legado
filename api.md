# 阅读API
## 对于Web的配置
您需要先在设置中启用"Web 服务"。  
## 使用
### Web
以下说明假设您的操作在本机进行，且开放端口为1234。  
如果您要从远程计算机访问[阅读]()，请将`127.0.0.1`替换成手机IP。
#### 插入单个书源
```
URL = http://127.0.0.1:1234/saveSource
Method = POST
```

请求BODY内容为`JSON`字符串，  
格式参考[这个文件](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/data/entities/BookSource.kt)

#### 插入多个书源or订阅源

```
URL = http://127.0.0.1:1234/saveBookSources
URL = http://127.0.0.1:1234/saveRssSources
Method = POST
```

请求BODY内容为`JSON`字符串，  
格式参考[这个文件](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/data/entities/BookSource.kt)，**为数组格式**。

#### 获取书源

```
URL = http://127.0.0.1:1234/getBookSource?url=xxx
URL = http://127.0.0.1:1234/getRssSource?url=xxx
Method = GET
``` 

#### 获取所有书源or订阅源

```
URL = http://127.0.0.1:1234/getBookSources
URL = http://127.0.0.1:1234/getRssSources
Method = GET
```

#### 删除多个书源or订阅源

```
URL = http://127.0.0.1:1234/deleteBookSources
URL = http://127.0.0.1:1234/deleteRssSources
Method = POST
```

请求BODY内容为`JSON`字符串，  
格式参考[这个文件](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/data/entities/BookSource.kt)，**为数组格式**。

#### 插入书籍
```
URL = http://127.0.0.1:1234/saveBook
Method = POST
```

请求BODY内容为`JSON`字符串，  
格式参考[这个文件](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/data/entities/Book.kt)。

#### 获取所有书籍
```
URL = http://127.0.0.1:1234/getBookshelf
Method = GET
```

获取APP内的所有书籍。  

#### 获取书籍章节列表
```
URL = http://127.0.0.1:1234/getChapterList?url=xxx
Method = GET
```

获取指定图书的章节列表。   

#### 获取书籍内容
```
URL = http://127.0.0.1:1234/getBookContent?url=xxx&index=1
Method = GET
```
获取指定图书的第`index`章节的文本内容。

#### 获取封面
```
URL = http://127.0.0.1:1234/cover?path=xxxxx
Method = GET
```


### Content Provider
* 需声明`io.legado.READ_WRITE`权限
* `providerHost`为`包名.readerProvider`, 如`io.legado.app.release.readerProvider`,不同包的地址不同,防止冲突安装失败
* 以下出现的`providerHost`请自行替换

#### 插入单个书源or订阅源

```
URL = content://providerHost/bookSource/insert
URL = content://providerHost/rssSource/insert
Method = insert
```

创建`Key="json"`的`ContentValues`，内容为`JSON`字符串，  
格式参考[这个文件](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/data/entities/BookSource.kt)

#### 插入多个书源or订阅源

```
URL = content://providerHost/bookSources/insert
URL = content://providerHost/rssSources/insert
Method = insert
```

创建`Key="json"`的`ContentValues`，内容为`JSON`字符串，  
格式参考[这个文件](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/data/entities/BookSource.kt)，**为数组格式**。

#### 获取书源or订阅源

```
URL = content://providerHost/bookSource/query?url=xxx
URL = content://providerHost/rssSource/query?url=xxx
Method = query
```

获取指定URL对应的书源信息。  
用`Cursor.getString(0)`取出返回结果。

#### 获取所有书源or订阅源

```
URL = content://providerHost/bookSources/query
URL = content://providerHost/rssSources/query
Method = query
```

获取APP内的所有书源。  
用`Cursor.getString(0)`取出返回结果。

#### 删除多个书源or订阅源

```
URL = content://providerHost/bookSources/delete
URL = content://providerHost/rssSources/delete
Method = delete
```

创建`Key="json"`的`ContentValues`，内容为`JSON`字符串，  
格式参考[这个文件](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/data/entities/BookSource.kt)，**为数组格式**。

#### 插入书籍
```
URL = content://providerHost/book/insert
Method = insert
```

创建`Key="json"`的`ContentValues`，内容为`JSON`字符串，  
格式参考[这个文件](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/data/entities/Book.kt)。

#### 获取所有书籍
```
URL = content://providerHost/books/query
Method = query
```

获取APP内的所有书籍。  
用`Cursor.getString(0)`取出返回结果。

#### 获取书籍章节列表
```
URL = content://providerHost/book/chapter/query?url=xxx
Method = query
```

获取指定图书的章节列表。   
用`Cursor.getString(0)`取出返回结果。

#### 获取书籍内容

```
URL = content://providerHost/book/content/query?url=xxx&index=1
Method = query
```
获取指定图书的第`index`章节的文本内容。     
用`Cursor.getString(0)`取出返回结果。

#### 获取封面
```
URL = content://providerHost/book/cover/query?path=xxxx
Method = query
```
