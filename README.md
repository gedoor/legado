
## LegadoEdge/阅读内置EdgeTTS 微软大声朗读


### 主要修改
- 修改音频流的暂存方式 (写硬盘=>写内存)
- 原作者来是把音频缓存硬盘上会频繁执行写入和删除(有多少段落就写多少次),
- 我不确定频繁执行写入会不会影响寿命或许对于现代存储来说影响微乎其微😋 我改成了放在内存中, 每读完一章就释放已读完的的媒体, 修改内容参见PR:gedoor/legado#5304

![detail.png](https://raw.githubusercontent.com/WangSunio/img/main/images/pre.png)

