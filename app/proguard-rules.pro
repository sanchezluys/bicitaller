# ProGuard / R8 optimization and obfuscation rules

# Preserve line numbers and source file names for readable Play Console crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve annotations and generics signatures
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Room Database and Entities
-keep class * extends androidx.room.RoomDatabase
-keep class com.example.data.model.** { *; }
-keep interface com.example.data.dao.** { *; }
-dontwarn androidx.room.paging.**

# Coroutines and Moshi / Retrofit
-dontwarn sun.misc.**
-dontwarn java.lang.invoke.**
-keepclassmembers class * {
    @com.squareup.moshi.* <fields>;
    @com.squareup.moshi.* <methods>;
}

