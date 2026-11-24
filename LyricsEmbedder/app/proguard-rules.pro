# Keep kotlinx.serialization classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.lyricsembedder.app.data.model.**$$serializer { *; }
-keepclassmembers class com.lyricsembedder.app.data.model.** {
    *** Companion;
}

# Keep JAudioTagger reflection classes
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**
