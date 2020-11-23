---
name: 软件BUG提交
about: 这是一个反馈模板，请按照自己的情况修改内容
---

### 机型
> 华为Mate7

### 安卓版本
> Android 7.1.1

### 阅读Legdao版本（我的-关于-版本）
> 3.20.112220

### 问题描述（简要描述发生的问题）
> 我发现了一个BUG (●'◡'●)

### 使用书源（填写链接或者JSON）
> 例1 https://booksources.github.io/

```json
例2
{
    "bookSourceUrl": "https://www.siluke.tw",
    "bookSourceType": "0",
    "bookSourceName": "思路客",
    "bookSourceGroup": "正则",
    "bookSourceComment": "",
    "loginUrl": "",
    "bookUrlPattern": "",
    "header": "",
    "searchUrl": "search.php?keyword={{key}}&page={{page}}",
    "exploreUrl": "",
    "enabled": false,
    "enabledExplore": false,
    "weight": 0,
    "customOrder": 15,
    "lastUpdateTime": 0,
    "ruleSearch": {
        "bookList": "//*[@class=\"result-list\"]/div",
        "name": "//a[@cpos=\"title\"]/@title",
        "author": "//*[text()=\"作者：\"]/following-sibling::*/text()",
        "kind": "//*[text()=\"类型：\"]/following-sibling::*/text()",
        "lastChapter": "//a[@cpos=\"newchapter\"]/text()",
        "coverUrl": "//img/@src",
        "bookUrl": "//a[@cpos=\"title\"]/@href"
    },
    "ruleExplore": {},
    "ruleBookInfo": {
        "name": "//*[@property='og:novel:book_name']/@content",
        "author": "//*[@property='og:novel:author']/@content",
        "kind": "//*[@property='og:novel:category']/@content",
        "lastChapter": "//*[@property='og:novel:latest_chapter_name']/@content",
        "intro": "//*[@property='og:description']/@content",
        "coverUrl": "//*[@property='og:image']/@content"
    },
    "ruleToc": {
        "chapterList": ":(?s)dd><a href=\"([^\"]*)\"[^>]*>([^<]*)",
        "chapterName": "$2",
        "chapterUrl": "$1"
    },
    "ruleContent": {
        "content": "//*[@id='content']"
    }
}
```

### 复现步骤（详细描述导致问题产生的操作步骤，如果能稳定复现）
> 例：打开阅读>点击我的>书源管理>右上角（本地导入）>选择自带文件选择器>软件显示白屏，看不到文件

### 日志提交（问题截图或者日志）
> 这个不用举例了，自己把问题出现时的界面截图，拖入页面内就会自动上传了。
