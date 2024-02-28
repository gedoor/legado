# 在线朗读规则说明

* 在线朗读规则为url规则,同书源url
* js参数
```
speakText //朗读文本
speakSpeed //朗读速度,5-50
```
* 例:
```
http://tts.baidu.com/text2audio,{
    "method": "POST",
    "body": "tex={{java.encodeURI(java.encodeURI(speakText))}}&spd={{String((speakSpeed + 5) / 10 + 4)}}&per=5003&cuid=baidu_speech_demo&idx=1&cod=2&lan=zh&ctp=1&pdt=1&vol=5&pit=5&_res_tag_=audio"
}
```