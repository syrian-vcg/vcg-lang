# VCG Editor ProGuard rules
-keep class com.syrianvcg.editor.** { *; }
-keepclassmembers class com.syrianvcg.editor.VcgJsInterface {
    @android.webkit.JavascriptInterface <methods>;
}
-dontwarn javax.**
