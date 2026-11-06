# VOID keeps nothing that R8 already handles; these rules only protect
# Compose's runtime hooks and the app entry point.
-keep class dev.voidapp.MainActivity { *; }
-keepclassmembers class androidx.compose.runtime.** { *; }
-dontwarn org.jetbrains.annotations.**
