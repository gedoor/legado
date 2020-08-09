#阅读API
## 对于Web的配置
您需要先在设置中启用"Web 服务"。  
## 使用
### Web
待补充
### Content Provider
#### 插入单个书源
```
URL = content://io.legado.app.api.ReaderProvider/source/insert
Method = insert
```

创建`Key="json"`的`ContentValues`，内容为`JSON`字符串，  
格式参考[这个文件](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/data/entities/BookSource.kt)

#### 插入多个书源
```
URL = content://io.legado.app.api.ReaderProvider/sources/insert
Method = insert
```

创建`Key="json"`的`ContentValues`，内容为`JSON`字符串，  
格式参考[这个文件](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/data/entities/BookSource.kt)，**为数组格式**。

#### 获取书源
```
URL = content://io.legado.app.api.ReaderProvider/source/query?url=xxx
Method = query
```

获取指定URL对应的书源信息。  
用`Cursor.getString(0)`取出返回结果。

#### 获取所有书源
```
URL = content://io.legado.app.api.ReaderProvider/sources/query
Method = query
```

获取APP内的所有书源。  
用`Cursor.getString(0)`取出返回结果。

#### 删除多个书源
```
URL = content://io.legado.app.api.ReaderProvider/sources/delete
Method = delete
```

创建`Key="json"`的`ContentValues`，内容为`JSON`字符串，  
格式参考[这个文件](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/data/entities/BookSource.kt)，**为数组格式**。

#### 插入书籍
```
URL = content://io.legado.app.api.ReaderProvider/book/insert
Method = insert
```

创建`Key="json"`的`ContentValues`，内容为`JSON`字符串，  
格式参考[这个文件](https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/data/entities/Book.kt)。

#### 获取所有书籍
```
URL = content://io.legado.app.api.ReaderProvider/books/query
Method = query
```

获取APP内的所有书籍。  
用`Cursor.getString(0)`取出返回结果。

#### 获取书籍章节列表
```
URL = content://io.legado.app.api.ReaderProvider/book/chapter/query?url=xxx
Method = query
```

获取指定图书的章节列表。   
用`Cursor.getString(0)`取出返回结果。

#### 获取书籍内容

```
URL = content://io.legado.app.api.ReaderProvider/book/content/query?url=xxx&index=1
Method = query
```

获取指定图书的第`index`章节的文本内容。     
用`Cursor.getString(0)`取出返回结果。