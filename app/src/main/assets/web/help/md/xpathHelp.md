# xpath 路径表达式详解

_注：本文所有代码均通过 Chrome(版本 123.0.6312.86) 验证_

> XPath 规范中定义了 13 种不同的轴（axes）。  
> 轴表示与元素的关系，并用于定位元素树上相对于该元素的元素。

-   `namespace`（不支持）
-   `attribute` 元素的属性。它可以缩写为 `@`
-   `self` 表示元素本身。它可以缩写为 `.`
-   `parent` 当前元素的父元素。它可以缩写为 `..`
-   `child` 当前元素的子元素。
-   `ancestor` 当前元素的所有直属祖先。
-   `ancestor-or-self` 当前元素及其所有直属祖先。
-   `descendant` 当前元素的所有递归子元素。
-   `descendant-or-self` 当前元素及其所有递归子元素。
-   `following` 当前元素之后出现的所有元素。无视元素层级，但不含直属后代。
-   `following-sibling` 当前元素之后出现的所有同级元素。
-   `preceding` 当前元素之前出现的所有元素。无视元素层级，但不含直属祖先。
-   `preceding-sibling` 当前元素之前出现的所有同级元素。

```js
// 轴的用法-> 轴名::表达式
// 例:
> $x('//body/ancestor-or-self::*')
< [body, html]
```

#### 一、xpath 表达式的基本格式

> xpath 通过"路径表达式"（Path Expression）来选取元素。  
> 在形式上，"路径表达式"与传统的文件系统非常类似。

```txt
# "/"斜杠作为路径内部的分割符。
# 同一个元素有绝对路径和相对路径两种写法。
# 绝对路径必须用"/"起首，后面紧跟根元素，比如/step/step/...。
# 相对路径则是除了绝对路径以外的其他写法，比如 step/step，也就是不使用"/"起首。
# "."表示当前元素。
# ".."表示当前元素的父元素
```

### 二、选取元素的基本规则

```txt
- "/"：表示选取根元素
- "//"：表示选取任意位置的某个元素
- nodename：表示选指定名称的元素
- "@"： 表示选取某个属性
```

### 三、选取元素的实例

```html
<!DOCTYPE html>
<html>
    <head>
        <meta charset="utf-8" />
        <title>标题</title>
        <meta property=author" content="作者" />
    </head>
    <body>
        <div>
            <title lang="eng">Harry Potter</title>
            <p>29.39</p>
            <p>usd</p>
        </div>
        <div>
            <title lang="cn">Cpp高级编程</title>
            <p>39.95</p>
            <p>rmb</p>
        </div>
        <div id="list">
            <dl>
                <dd><a href="/1">一</a></dd>
                <dd><a href="/2">二</a></dd>
                <dd><a href="/3">三</a></dd>
            </dl>
        </div>
    </body>
</html>
```

```js
// 例1
> $x('/') // 选取根元素,返回包含被选中元素的数组。
< [document]
// 例2
> $x('/html') // 选取根元素下的所有 html 子元素，这是绝对路径写法。
< [html]
// 例3
> $x('html/head/meta') // 选取 head 元素下的所有 meta 元素，这是相对路径写法。
< [meta, meta]  // <meta charset="utf-8">, <meta property="author" content="作者">
// 例4
> $x('//p') // 选取所有 p 元素，不管它们在哪里
< [p, p, p, p] // <p>29.39</p>, <p>usd</p>, <p>39.95</p>, <p>rmb</p>
// 例5
> $x('html/body//a') // 选取 body 元素下的所有 a 元素
< [a, a, a] // <a href="/1">一</a>, <a href="/2">二</a>, <a href="/3">三</a>
// 例6
> $x('//@lang') // 选取所有名为 lang 的属性。
< [lang, lang] // lang="eng", lang="cn"
> $x('html/head/meta/@content') // 选取 head 元素下所有 meta 元素的 content 属性。
< [content] // content="作者"
// 例7
> $x('//meta/..') // 选取所有 meta 元素的父元素。（相同的结果只会返回一个）
< [head] // <head>...</head>
```

### 四、xpath 的谓语条件（Predicate）

> 所谓"谓语条件"，就是对路径表达式的附加条件。  
> 所有的附加条件，都写在方括号 `[]` 中，用于对元素进一步的筛选。
> 方括号内的表达式结果为 true 的元素才会被选取。

```js
// 例8
> $x('html/head/meta[1]') //  选取 head 元素下的第一个 meta 元素
< [meta] // <meta charset="utf-8">
> $x('//p[1]') // 选取所有元素下的第一个 p 元素
< [p, p] // <p>29.39</p>, <p>39.95</p>
// 例9
> $x('html/head/meta[last()]') // 选取 head 元素下的最后一个 meta 元素
< [meta] // <meta property="author" content="作者">
// 例10
> $x('html/head/meta[last()-1]') // 选取 head 元素下的倒数第二个 meta 元素
< [meta] // <meta charset="utf-8">
// 例11
> $x('html/head/meta[position()>1]') // 选取 head 元素下的除了第一个元素外的所有 meta 元素
< [meta] // <meta property="author" content="作者">
// 例12
> $x('//title[@lang]') // 选取所有具有lang属性的title元素。
< [title, title] // <title lang="eng">Harry Potter</title>, <title lang="cn">Cpp高级编程</title>
// 例13
> $x('//title[@lang="eng"]') // 选取所有lang属性的值等于"eng"的title元素。
< [title] // <title lang="eng">Harry Potter</title>
// 例14
> $x('/html/body/div[dl]') // 选择 body 的 div 子元素，且被选中 的 div 元素必须带有 dl 子元素。
< [div] // <div id="list"><dl id="list">...</dl></div>
// 例15
> $x('/html/body/div[p>35.00]') // 选取 body 的 div 子元素，且被选中 div 元素的 p 子元素的值必须大于 35.00。
< [div] // <div><title lang="cn">Cpp高级编程</title><p>39.95</p><p>rmb</p></div>
> $x('/html/body/div[p="rmb"]') // 选取 body 的 div 子元素，且被选中 div 元素的 p 子元素的值必须等于 "rmb"。
< [div] // <div><title lang="cn">Cpp高级编程</title><p>39.95</p><p>rmb</p></div>
// 例16
> $x('/html/body/div[p="rmb"]/title') // 在例14结果集中，选择title子元素。
< [title] // <title lang="cn">Cpp高级编程</title>
// 例17
> $x('/html/body/div/p[.>35.00]') // 选择值大于 35.00 的 "/html/body/div" 的 p 子元素。
< [p] // <p>39.95</p>
```

### 五、通配符

-   `\*` 表示匹配任何元素。
-   `@\*` 表示匹配任何属性名。

```js
// 例18
> $x('//*') // 选取所有元素，结果以递归顺序返回
< [html, head, meta, title, meta, body, div, title, p, p, div, title, p, p, div, dl, dd, a, dd, a, dd, a]
// 例19
> $x('/*/*') // 选取所有第二层的元素
< [head, body] // <head>...</head>, <body>...</body>
// 例20
> $x('//dl[@id="list"]/*') // 选取 id="list" 的 dl 元素的所有子元素。
< [dd, dd, dd] // <dd><a href="/1">一</a></dd>, <dd><a href="/2">二</a></dd>, <dd><a href="/3">三</a></dd>
// 例21
> $x('//title[@*]') // 选取所有带有属性的 title 元素。
< [title, title] // <title lang="eng">Harry Potter</title>, <title lang="cn">Cpp高级编程</title>
```

### 六、选择多个路径

-   用 `|` 合并多个表达式的选取结果。

```js
// 例22
> $x('//title | //a') // 选取所有 title 和 a 元素。
< [title, title, title, a, a, a]

```

### 七、xpath 的函数

> xpath 函数的参数可以是静态字符串或表达式，且函数可以嵌套调用。  
> xpath 的索引均从1开始，而不是从0开始。

```js
// boolean(expression) 将表达式选取的结果转换为布尔值。
> $x('boolean(//title)')
< true
// number([object]) 将表达式选取的结果转换为数字。(HTML元素内容默认均为字符串)
> $x('number(//p[1])')
< 29.39
// round(decimal) 将数字参数转换为整数并四舍五入。
> $x('round(//p[1])')
< 29
// ceiling(number) 将数字参数转换为整数并向上取整。ceiling(5.2)=6
> $x('ceiling(//p[1])') // 仅使用匹配表达式的第一个元素
< 30
// floor(number) 将数字参数转换为整数并向下取整。floor(5.8)=5
> $x('floor(//p[1])')
< 29
// concat( string1, string2 [,stringn]* ) 字符串拼接，参数为静态字符串或表达式
> $x('concat("cost:", //p[1], //p[2])') // 仅使用匹配表达式的第一个元素
< 'cost:29.39usd'
// contains(haystack, needle) 判断 haystack 是否包含 needle，返回 boolean
> $x('contains(//p[1], "29.39")') // 仅使用匹配表达式的第一个元素
< true
> $x('//title[contains(., "Harry")]') // 选取内容中包含 "Harry" 的 title 元素。
< [title] // <title lang="eng">Harry Potter</title>
// count( node-set ) 统计表达式选取的元素个数。
> $x('count(//p)')
< 4
// id(expression) 根据 id 属性选取元素，若参数为表达式，将获取表达式结果作为id查询。
> $x('id(//dl/@id)') // 等效于 $x('id("list")')
< [dl#list] // <dl id="list">...</dl>
// last() 返回当前路径表达式匹配的同级元素集合的成员数量。
> $x('//p[last()]')
< [p, p] // <p>usd</p>, <p>rmb</p>
// name([node-set]) 返回表达式选取集合的首个成员带命名空间的元素名，HTML中与local-name([node-set])等价。
// local-name([node-set]) 返回表达式选取集合的首个成员本地元素名。
> $x('local-name(//*[@id])') //
< 'dl'
// namespace-uri([node-set]) 获取选定节点集中第一个节点的命名空间URI。
> $x('namespace-uri(//div)')
< 'http://www.w3.org/1999/xhtml' // HTML通常都返回这个固定值
// normalize-space([string]) 去文本内容中的前后空白以及将内部连续的空白替换为单个空格
> $x('normalize-space("  test    string   ")')
< 'test string'
// not(expression) 返回表达式的布尔反值。
> $x('//title[not(@lang)]')
< [title] // <title>标题</title>
// position() 返回选定元素处于路径表达式匹配的同级元素集合中的位置。
> $x('//meta[position()=2]')
< [meta] // <meta property=author" content="作者" />
// starts-with(haystack, needle) 检查某个字符串 haystack 是否以另一个字符串 needle 开始。
> $x('//title[starts-with(., "Cpp")]')
< [title] // <title lang="cn">Cpp高级编程</title]
// string([object]) 将给定参数转换为字符串
> $x('string(//p)')
< '29.39'
// string-length([string]) 返回给定字符串的字符数量
> $x('string-length(string(//p))')
< 5
// substring(string, start[, length]) 截取字符串
> $x('substring(string(//p), 1, 3)')
< '29.'
// substring-after(haystack, needle) 返回字符串 haystack 中第一个 needle 之后的字符串。
> $x('substring-after(string(//p), ".")')
< '39'
// substring-before(haystack, needle) 返回字符串 haystack 中第一个 needle 之前的字符串。
> $x('substring-before(string(//p), ".")')
< '29'
// sum([node-set]) 对给定集合的数字求和。若给定集合中存在非数字，则返回 NaN
> $x('sum(//p[1])')
< 69.34
// translate(string, "abc", "XYZ") 依次替换 string 中出现的 a、b、c 为对应位置的 X、Y、Z。
// 若第三个参数中的字符少于第二个参数，那么在第一个参数中相应的字符将被删除。
> $x('translate("aabbcc112233", "ac2", "V8")')
< 'VVbb881133'
// true() 表示函数中的 true 布尔值
// false() 表示函数中的 false 布尔值
```
