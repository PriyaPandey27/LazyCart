# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the AGP default proguard file.

# Room — keep entity and DAO classes
-keep class com.LazyCart.app.data.local.db.** { *; }

# Hilt — keep generated components
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.** { *; }

# Keep Kotlin coroutines metadata
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# DataStore — keep generated classes
-keep class androidx.datastore.** { *; }

# Navigation Compose — keep destination routes
-keepclassmembers class * extends androidx.navigation.NavGraph { *; }
