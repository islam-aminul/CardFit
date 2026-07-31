# R8 keep rules for the minified release build.
#
# These are load-bearing: with an empty rules file the release build crashed on launch with
#   NullPointerException reading com.google.mlkit.vision.segmentation.subject.internal.zzc.a
# from MLTaskExecutor's constructor, because R8 had stripped statically-initialised fields out of
# ML Kit's internals. Debug builds never see this — minification only runs for release.

# --- ML Kit / Play services --------------------------------------------------
# ML Kit resolves implementations reflectively through its component registry, so its classes and
# their members must survive shrinking and obfuscation. This covers the document scanner, bundled
# text recognition, and the (beta) subject segmentation used for background removal.
-keep class com.google.mlkit.** { *; }
-keep interface com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-keep class com.google.android.gms.common.annotation.** { *; }
-dontwarn com.google.mlkit.**

# --- kotlinx.serialization ---------------------------------------------------
# Task/Application-set JSON is serialized reflectively via generated $$serializer classes and
# Companion serializer() methods. Without these the set persistence would fail only in release.
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class in.firm.consultancy.bayaan.cardfit.**$$serializer { *; }
-keepclassmembers class in.firm.consultancy.bayaan.cardfit.** { *** Companion; }
-keepclasseswithmembers class in.firm.consultancy.bayaan.cardfit.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Enums persisted by name -------------------------------------------------
# Preferences and the per-type export blobs store enum constants by .name and read them back with
# valueOf, so the constant names must not be renamed.
-keepclassmembers enum in.firm.consultancy.bayaan.cardfit.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public static ** getEntries();
}
