# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# 混合时不使用大小写混合，混合后的类名为小写
-dontusemixedcaseclassnames

# 指定不去忽略非公共库的类
-dontskipnonpubliclibraryclasses

# 这句话能够使我们的项目混淆后产生映射文件
# 包含有类名->混淆后类名的映射关系
-verbose

# 指定不去忽略非公共库的类成员
-dontskipnonpubliclibraryclassmembers

# 不做预校验，preverify是proguard的四个步骤之一，Android不需要preverify，去掉这一步能够加快混淆速度。
-dontpreverify

# 保留Annotation不混淆
-keepattributes *Annotation*,InnerClasses

# 避免混淆泛型
-keepattributes Signature

# 抛出异常时保留代码行号
-keepattributes SourceFile,LineNumberTable

# 指定混淆是采用的算法，后面的参数是一个过滤器
# 这个过滤器是谷歌推荐的算法，一般不做更改
-optimizations !code/simplification/cast,!field/*,!class/merging/*


#############################################
#
# Android开发中一些需要保留的公共部分
#
#############################################
# 屏蔽错误Unresolved class name
#noinspection ShrinkerUnresolvedReference

# 保留我们使用的四大组件，自定义的Application等等这些类不被混淆
# 因为这些子类都有可能被外部调用
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View

# 保留androidx下的所有类及其内部类
-keep class androidx.** {*;}

# 保留继承的
-keep public class * extends androidx.**

# 保留R下面的资源
-keep class **.R$* {*;}

# 保留本地native方法不被混淆
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留在Activity中的方法参数是view的方法，
# 这样以来我们在layout中写的onClick就不会被影响
-keepclassmembers class * extends android.app.Activity{
    public void *(android.view.View);
}

# 保留枚举类不被混淆
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留我们自定义控件（继承自View）不被混淆
-keep public class * extends android.view.View{
    *** get*();
    void set*(***);
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# 保留Parcelable序列化类不被混淆
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# 保留Serializable序列化的类不被混淆
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 对于带有回调函数的onXXEvent、**On*Listener的，不能被混淆
-keepclassmembers class * {
    void *(**On*Event);
    void *(**On*Listener);
}

# webView处理，项目中没有使用到webView忽略即可
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String);
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
}

# 移除Log类打印各个等级日志的代码，打正式包的时候可以做为禁log使用，这里可以作为禁止log打印的功能使用
# 记得proguard-android.txt中一定不要加-dontoptimize才起作用
# 另外的一种实现方案是通过BuildConfig.DEBUG的变量来控制
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# 保持js引擎调用的java类
-keep class * extends io.legado.app.help.JsExtensions{*;}
# 保持js引擎调用的java类
-keep class **.analyzeRule.**{*;}
# 保持web类
-keep class **.web.**{*;}
# 数据类
-keep class **.data.**{*;}
# hutool-core hutool-crypto
-keep class cn.hutool.core.**{*;}
-keep class cn.hutool.crypto.**{*;}
-dontwarn cn.hutool.**
# 缓存 Cookie
-keep class **.help.http.CookieStore{*;}
-keep class **.help.CacheManager{*;}
# StrResponse
-keep class **.help.http.StrResponse{*;}

-dontwarn rx.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.apache.log4j.lf5.viewer.**
-dontnote org.apache.log4j.lf5.viewer.**
-dontwarn freemarker.**
-dontnote org.python.core.**
-dontwarn com.hwangjr.rxbus.**
-dontwarn okhttp3.**
-dontwarn org.conscrypt.**
-dontwarn com.jeremyliao.liveeventbus.**
-dontwarn org.commonmark.ext.gfm.**

-keep,allowobfuscation,allowshrinking class com.google.gson.** { *; }
-keep,allowobfuscation,allowshrinking class com.ke.gson.** { *; }
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken
-keep class com.jeremyliao.liveeventbus.** { *; }
-keep class okhttp3.**{*;}
-keep class okio.**{*;}
-keep class com.hwangjr.rxbus.**{*;}
-keep class org.conscrypt.**{*;}
-keep class android.support.**{*;}
-keep class me.grantland.widget.**{*;}
-keep class de.hdodenhof.circleimageview.**{*;}
-keep class tyrant.explosionfield.**{*;}
-keep class tyrantgit.explosionfield.**{*;}
-keep class freemarker.**{*;}
-keep class com.gyf.barlibrary.** {*;}

## JSOUP
-keep class org.jsoup.**{*;}
-keep class **.xpath.**{*;}
-dontwarn org.jspecify.annotations.NullMarked

-keep class org.slf4j.**{*;}
-dontwarn org.slf4j.**

-keep class org.codehaus.**{*;}
-dontwarn org.codehaus.**
-keep class com.jayway.**{*;}
-dontwarn com.jayway.**
-keep class com.fasterxml.**{*;}

-keep class javax.swing.**{*;}
-dontwarn javax.swing.**
-keep class java.awt.**{*;}
-dontwarn java.awt.**
-keep class sun.misc.**{*;}
-dontwarn sun.misc.**
-keep class sun.reflect.**{*;}
-dontwarn sun.reflect.**

## Rhino
-keep class com.script.** { *; }
-keep class javax.script.** { *; }
-keep class java.lang.** { *; }
-keep class java.util.function.** { *; }
-keep class com.sun.script.javascript.** { *; }
-keep class org.mozilla.** { *; }
-dontwarn org.mozilla.javascript.engine.RhinoScriptEngineFactory

## EPUB
-dontwarn nl.siegmann.epublib.**
-dontwarn org.xmlpull.**
-keep class nl.siegmann.epublib.**{*;}
-keep class javax.xml.**{*;}
-keep class org.xmlpull.**{*;}

-keep class org.simpleframework.**{*;}
-dontwarn org.simpleframework.xml.**

-keepclassmembers class * {
    public <init> (org.json.JSONObject);
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

## ExoPlayer 反射设置ua 保证该私有变量不被混淆
-keepclassmembers class androidx.media3.datasource.cache.CacheDataSource$Factory {
    *** upstreamDataSourceFactory;
}
## ExoPlayer 如果还不能播放就取消注释这个
# -keep class com.google.android.exoplayer2.** {*;}

## 对外提供api
-keep class io.legado.app.api.ReturnData{*;}

# 繁简转换
-keep class com.github.liuyueyi.quick.transfer.** {*;}

# Cronet
-keepclassmembers class org.chromium.net.X509Util {
    *** sDefaultTrustManager;
    *** sTestTrustManager;
}

# Class.forName调用
-keep class io.legado.app.lib.cronet.CronetInterceptor{*;}
-keep class io.legado.app.lib.cronet.CronetLoader{*;}
-keep class io.legado.app.help.AppUpdateGitHub{*;}
-keep class io.legado.app.help.AppIntentType{*;}
# Error Exception 
-keep class * extends java.lang.Exception
-keep class * extends java.lang.Error
-keep class **Exception

