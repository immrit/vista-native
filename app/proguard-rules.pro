# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Foundation entry-flow classes are kept as class boundaries so release stack
# traces and the reproducible R8 gate can still identify Startup/Auth owners
# after moving their implementation to feature modules. Members may still be
# optimized and names may still be obfuscated.
-keep,allowoptimization,allowobfuscation class ir.coffevista.vista_native.features.startup.StartupResolver { *; }
-keep,allowoptimization,allowobfuscation class ir.coffevista.vista_native.features.auth.AuthViewModel { *; }
-keep,allowoptimization,allowobfuscation class ir.coffevista.vista_native.features.auth.data.OkHttpAuthRemoteDataSource { *; }
