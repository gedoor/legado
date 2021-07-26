/**
 * wifi传书功能，供客户端调用
 * 切换语言环境	addby zhongweikang@zhangyue.com  2015/09/23
 */

 var language;//当前语言
 var jsonEn;//英文
 var jsonZh;//中文
 
 /**
  * 语言包
  */
 jsonEn = {	t0:"WiFi Upload",t1:"Documents",t2:"Name",t3:"Size",t4:"Operation", 
             t5:"Supported Format：TXT、EPUB、UMD",t6:"Drag here and Drop to upload",
             t7:"You choosed [txt] files ,we only can upload [txt]  of them。\nPlease choose TXT、EPUB、UMD files, file name cannot be repeated.",
             t8:"Failed",t9:"Cancel",t10:"Done",t11:"One of books is over 500MB",t12:"Bad file mode",
             t13:"File already exists",t14:"Canceled"
         };
 jsonZh = {	t0:"WiFi 下载",t1:"文档",t2:"名字",t3:"大小",t4:"操作",
             t5:"支持类型：TXT、EPUB、UMD",t6:"拖至此处上传",
             t7:"你选择了[txt]个文件，我们只能上传其中的[txt]。\n请选择 TXT、EPUB、UMD 类型的文件，文件的名字不能重复。",
             t8:"失败",t9:"取消",t10:"完成",t11:"上传文件不能大于500MB",t12:"无效的文件格式",
             t13:"文件已存在",t14:"已取消"
         };
 
 if (navigator.language) {
     language = navigator.language;
 }
 else {
     language = navigator.browserLanguage;
 }
 language = language.toLowerCase(language);
 
 /**
  * 切换语言
  */
 function langSwich(s){
     switch(s){
         case 'en-us':
             _strReplace(jsonEn);
             break;
         case 'zh-cn':
             _strReplace(jsonZh);
             break;
         default:
             _strReplace(jsonEn);
             break;
     }	
 }
 function _strReplace(d){
     window.jsonLang=d;//全局变量在其他js文件中会遇到，勿删。
     for(var i in jsonLang){
         $('[data-js=' + i + ']').html(d[i])
     }
 }
 
 /**
  * 特殊字符串替换，例：语言包中的 t7
  * content	替换前的文本
  * replace  待插入的文本 （支持变量或数组）
  */
 var isArray = function(obj) { 
     return Object.prototype.toString.call(obj) === '[object Array]'; 
 }
 function stringReplace(content,replace){
 
     var str = content;
     if(!str){return null;}
     
     if(isArray(replace)){
         strs=str.split("[txt]");
         count = strs.length-1;
         
         var string = '';
         for(var i=0;i<count;i++){
             string = string+strs[i]+replace[i];
         }
         string = string+strs[count];	
     }else{
         string=str.replace(replace,"[txt]"); 
     }
     return string;
 }
 
 
 //执行
 langSwich(language);
 