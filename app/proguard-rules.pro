# Keep ExoPlayer classes that are loaded reflectively
-keep class androidx.media3.** { *; }
# kotlinx.serialization keeps
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class co.loopr.player.api.**$$serializer { *; }
-keepclassmembers class co.loopr.player.api.** { *** Companion; }
-keepclasseswithmembers class co.loopr.player.api.** { kotlinx.serialization.KSerializer serializer(...); }
