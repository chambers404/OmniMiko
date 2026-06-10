# Keep kotlinx.serialization generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.omnimiko.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.omnimiko.**$$serializer { *; }

# MediaPipe GenAI native bindings.
-keep class com.google.mediapipe.** { *; }
-keep class com.google.protobuf.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**
