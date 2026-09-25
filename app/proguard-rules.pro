# Release keep rules for reflective engine paths used by photo/movie export.

-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class com.pixijoy.** { *; }

# Media3 / CompositionPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# MediaPipe subject mask
-keep class com.google.mediapipe.** { *; }
-keep class com.google.common.flogger.** { *; }
-dontwarn com.google.mediapipe.**
-dontwarn com.google.common.flogger.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# WorkManager export workers
-keep class * extends androidx.work.ListenableWorker
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker

# CameraX
-keep class androidx.camera.** { *; }

# HEIC writer fallback
-keep class androidx.heifwriter.** { *; }

# Annotation processors pulled into MediaPipe / AutoValue
-dontwarn javax.lang.model.SourceVersion
-dontwarn javax.lang.model.element.Element
-dontwarn javax.lang.model.element.ElementKind
-dontwarn javax.lang.model.type.TypeMirror
-dontwarn javax.lang.model.type.TypeVisitor
-dontwarn javax.lang.model.util.SimpleTypeVisitor8

# Play services LiteRT (InterpreterApi)
-keep class org.tensorflow.lite.** { *; }
-keep class com.google.android.gms.tflite.** { *; }
-dontwarn org.tensorflow.lite.**
-dontwarn com.google.android.gms.tflite.**
-keep class com.google.android.play.core.** { *; }
-dontwarn com.google.android.play.core.**

# Kotlin / coroutines
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
