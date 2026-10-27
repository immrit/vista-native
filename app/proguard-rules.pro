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

# protobuf-javalite message metadata stores generated field names as strings.
# R8 may still optimize and obfuscate the generated class, but these field
# names must remain stable or AppPreferences fails during static initialization.
-keepclassmembers,allowoptimization class ir.coffevista.vista_native.core.datastore.proto.** extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# Chat feature E2EE security, models, and DTOs
-keep class ir.coffevista.vista_native.features.chat.data.security.** { *; }
-keep class ir.coffevista.vista_native.features.chat.domain.model.** { *; }
-keep class ir.coffevista.vista_native.features.chat.data.remote.** { *; }
-keep class ir.coffevista.vista_native.features.chat.data.local.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    public static final ** Companion;
}
-keepclassmembers class * {
    public static final **$$serializer INSTANCE;
}
-keepclassmembers class * extends kotlinx.serialization.KSerializer {
    <init>(...);
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <init>(...);
}
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Crypto and security providers
-keepclassmembers class * extends java.security.Provider { *; }

# Cafe Bazaar Poolakey In-App Billing
-keep class ir.cafebazaar.poolakey.** { *; }
-keep class com.android.vending.billing.** { *; }


