# Applied only with -PtestRelease. The instrumentation runner and Compose test APIs execute inside the
# app process and expect these libraries to be present unshrunk; app code, Haze, ZXing and
# kotlinx.serialization are still processed by R8 exactly as in the shipping build.
-keep class androidx.** { *; }
-keep class org.junit.** { *; }
-keep class junit.** { *; }
-keep class org.hamcrest.** { *; }
-keep class kotlin.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keep class com.chiforest.forestflow.** { *; }
-dontwarn **
-keep class javax.** { *; }
-keep class dagger.** { *; }
-keep class com.google.common.** { *; }
-keep class org.ccil.** { *; }
-keep class org.jspecify.** { *; }
-keep class kotlinx.serialization.** { *; }
