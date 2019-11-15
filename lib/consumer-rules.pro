-optimizationpasses 5
-ignorewarnings
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
-useuniqueclassmembernames
-allowaccessmodification
-dontpreverify
-verbose
-dontoptimize
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes *Annotation*
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}
-keepclassmembers enum * {
  public static **[] values();
  public static ** valueOf(java.lang.String);
}
-keep public class **.R$*{
    public static final int *;
}
-keepclassmembers class **.R$* {
    public static <fields>;
}
-keepclassmembers class * {
    native <methods>;
}
-keepclasseswithmembernames class * {
    native <methods>;
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(***);
    public *** set*(***);
    public *** get*(***);
    public *** get*();
}
-keep class android.support.annotation.Keep
-keep @android.support.annotation.Keep class * {*;}
-keepclasseswithmembers class * {
    @android.support.annotation.Keep <methods>;
}
-keepclasseswithmembers class * {
    @android.support.annotation.Keep <fields>;
}
-keepclasseswithmembers class * {
    @android.support.annotation.Keep <init>(...);
}
-keep public class * implements android.os.Parcelable{*;}
-keepclasseswithmembers class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-dontwarn com.alibaba.fastjson.**
-keep class com.alibaba.fastjson.**{*;}

-keep class org.litepal.** {
    *;
}

-keep class * extends org.litepal.crud.DataSupport {
    *;
}

-keep class * extends org.litepal.crud.LitePalSupport {
    *;
}

-keepattributes *JavascriptInterface*
-keepclassmembers class * {
@android.webkit.JavascriptInterface <methods>;
}

-keep class com.chineseall.epubparser.lib.core.BookServiceDelegate$BookJSBridge
-keepclassmembers class com.chineseall.epubparser.lib.core.BookServiceDelegate$BookJSBridge$BookJSBridge {
    *;
}

-keep class com.chineseall.epubparser.lib.book.*{*;}
-keep class com.chineseall.epubparser.lib.html.*{*;}
-keep class com.chineseall.epubparser.lib.request.*{*;}