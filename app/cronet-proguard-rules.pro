# -------- Config Path: base/android/proguard/chromium_code.flags --------
# Copyright 2016 The Chromium Authors
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

# Contains flags that can be safely shared with Cronet, and thus would be
# appropriate for third-party apps to include.

# Allow unused native methods to be removed, but prevent renaming on those that are kept.
-keepclasseswithmembernames,includedescriptorclasses,allowaccessmodification class !cr_allowunused,** {
  native <methods>;
}

# Use assumevalues block instead of assumenosideeffects block because Google3 proguard cannot parse
# assumenosideeffects blocks which overwrite return value.
# chromium_code.flags rather than remove_logging.flags so that it's included
# in cronet.
-assumevalues class org.chromium.base.Log {
  static boolean isDebug() return false;
}

# Keep all CREATOR fields within Parcelable that are kept.
-keepclassmembers class !cr_allowunused,org.chromium.** implements android.os.Parcelable {
  public static *** CREATOR;
}

# Don't obfuscate Parcelables as they might be marshalled outside Chrome.
# If we annotated all Parcelables that get put into Bundles other than
# for saveInstanceState (e.g. PendingIntents), then we could actually keep the
# names of just those ones. For now, we'll just keep them all.
-keepnames,allowaccessmodification class !cr_allowunused,org.chromium.** implements android.os.Parcelable {}

# Keep all enum values and valueOf methods. See
# http://proguard.sourceforge.net/index.html#manual/examples.html
# for the reason for this. Also, see http://crbug.com/248037.
-keepclassmembers enum !cr_allowunused,org.chromium.** {
    public static **[] values();
}

# -identifiernamestring doesn't keep the module impl around, we have to
# explicitly keep it.
-if @org.chromium.components.module_installer.builder.ModuleInterface interface *
-keep,allowobfuscation,allowaccessmodification class !cr_allowunused,** extends <1> {
  <init>();
}

# Required to remove fields until b/274802355 is resolved.
-assumevalues class !cr_allowunused,** {
  final org.chromium.base.ThreadUtils$ThreadChecker * return _NONNULL_;
}

# TODO(agrieve): Remove once we start to use Android U SDK.
-dontwarn android.window.BackEvent
-dontwarn android.window.OnBackAnimationCallback
# -------- Config Path: build/android/chromium_annotations.flags --------
# Copyright 2022 The Chromium Authors
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

# Contains flags related to annotations in //build/android that can be safely
# shared with Cronet, and thus would be appropriate for third-party apps to
# include.

# Keep all annotation related attributes that can affect runtime
-keepattributes RuntimeVisible*Annotations
-keepattributes AnnotationDefault

# Keeps for class level annotations.
-keep,allowaccessmodification @org.chromium.build.annotations.UsedByReflection class ** {}

# Keeps for method level annotations.
-keepclasseswithmembers,allowaccessmodification class ** {
  @org.chromium.build.annotations.UsedByReflection <methods>;
}
-keepclasseswithmembers,allowaccessmodification class ** {
  @org.chromium.build.annotations.UsedByReflection <fields>;
}

# Never inline classes, methods, or fields with this annotation, but allow
# shrinking and obfuscation.
# Relevant to fields when they are needed to store strong references to objects
# that are held as weak references by native code.
-if @org.chromium.build.annotations.DoNotInline class * {
    *** *(...);
}
-keep,allowobfuscation,allowaccessmodification class <1> {
    *** <2>(...);
}
-keepclassmembers,allowobfuscation,allowaccessmodification class * {
   @org.chromium.build.annotations.DoNotInline <methods>;
}
-keepclassmembers,allowobfuscation,allowaccessmodification class * {
   @org.chromium.build.annotations.DoNotInline <fields>;
}

-alwaysinline class * {
    @org.chromium.build.annotations.AlwaysInline *;
}

# Keep all logs (Log.VERBOSE = 2). R8 does not allow setting to 0.
-maximumremovedandroidloglevel 1 class ** {
   @org.chromium.build.annotations.DoNotStripLogs <methods>;
}
-maximumremovedandroidloglevel 1 @org.chromium.build.annotations.DoNotStripLogs class ** {
   <methods>;
}

# Never merge classes horizontally or vertically with this annotation.
# Relevant to classes being used as a key in maps or sets.
-keep,allowaccessmodification,allowobfuscation,allowshrinking @org.chromium.build.annotations.DoNotClassMerge class *

# Mark members annotated with IdentifierNameString as identifier name strings
-identifiernamestring class * {
    @org.chromium.build.annotations.IdentifierNameString *;
}
# -------- Config Path: components/cronet/android/cronet_impl_common_proguard.cfg --------
# Proguard config for apps that depend on cronet_impl_common_java.jar.

# Used through reflection by the API code to figure out the version of the impl
# code it's talking to.
-keep public class org.chromium.net.impl.ImplVersion {
  public *;
}

-dontwarn com.google.errorprone.annotations.DoNotMock
# -------- Config Path: components/cronet/android/cronet_impl_native_proguard.cfg --------
# Proguard config for apps that depend on cronet_impl_native_java.jar.

# This constructor is called using the reflection from Cronet API (cronet_api.jar).
-keep class org.chromium.net.impl.NativeCronetProvider {
    public <init>(android.content.Context);
}

# While Chrome doesn't need to keep these with their version of R8, some cronet
# users may be on other optimizers which still require the annotation to be
# kept in order for the keep rules to work.
-keep @interface org.chromium.build.annotations.DoNotInline
-keep @interface org.chromium.build.annotations.UsedByReflection
-keep @interface org.chromium.build.annotations.IdentifierNameString
-keep @interface org.jni_zero.AccessedByNative
-keep @interface org.jni_zero.CalledByNative
-keep @interface org.jni_zero.CalledByNativeUnchecked

# Suppress unnecessary warnings.
-dontnote org.chromium.net.ProxyChangeListener$ProxyReceiver
-dontnote org.chromium.net.AndroidKeyStore
# Needs 'void setTextAppearance(int)' (API level 23).
-dontwarn org.chromium.base.ApiCompatibilityUtils
# Needs 'boolean onSearchRequested(android.view.SearchEvent)' (API level 23).
-dontwarn org.chromium.base.WindowCallbackWrapper

# Generated for chrome apk and not included into cronet.
-dontwarn org.chromium.base.multidex.ChromiumMultiDexInstaller
-dontwarn org.chromium.base.library_loader.LibraryLoader
-dontwarn org.chromium.base.SysUtils
-dontwarn org.chromium.build.NativeLibraries

# Objects of this type are passed around by native code, but the class
# is never used directly by native code. Since the class is not loaded, it does
# not need to be preserved as an entry point.
-dontnote org.chromium.net.UrlRequest$ResponseHeadersMap
# https://android.googlesource.com/platform/sdk/+/marshmallow-mr1-release/files/proguard-android.txt#54
-dontwarn android.support.**

# This class should be explicitly kept to avoid failure if
# class/merging/horizontal proguard optimization is enabled.
-keep class org.chromium.base.CollectionUtil

# Skip protobuf runtime check for isOnAndroidDevice().
# A nice-to-have optimization shamelessly stolen from //third_party/protobuf/java/lite/proguard.pgcfg.
-assumevalues class com.google.protobuf.Android {
    static boolean ASSUME_ANDROID return true;
}

# See crbug.com/1440987. We must keep every native that we are manually
# registering. If Cronet bumps its min-sdk past 21, we may be able to move to
# automatic JNI registration.
-keepclasseswithmembers,includedescriptorclasses,allowaccessmodification class org.chromium.**,J.N {
  native <methods>;
}

# Protobuf builder uses reflection so make sure ProGuard leaves it alone. See
# https://crbug.com/1395764.
# Note that we can't simply use the rule from
# //third_party/protobuf/java/lite/proguard.pgcfg, because some users who
# consume our ProGuard rules do not want all their protos to be kept. Instead,
# use a more specific rule that covers Chromium protos only.
-keepclassmembers class org.chromium.** extends com.google.protobuf.GeneratedMessageLite {
  <fields>;
}
# -------- Config Path: components/cronet/android/cronet_shared_proguard.cfg --------
# Proguard config for apps that depend on cronet_shared_java.jar (which should
# be all apps that depend on any part of Cronet)

# Part of the Android System SDK, so ProGuard won't be able to resolve it if
# running against the standard SDK.
-dontwarn android.util.StatsEvent
-dontwarn android.util.StatsEvent$*
# There is also an undefined reference to android.util.StatsLog.write(), which
# R8 appears to be fine with but other processors (e.g. internal Google
# ProGuard) may not be. See b/315269496.
-dontwarn android.util.StatsLog
# -------- Config Path: third_party/androidx/androidx_annotations.flags --------
# Copyright 2023 The Chromium Authors
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

-keep @androidx.annotation.Keep class *
-keepclasseswithmembers,allowaccessmodification class * {
  @androidx.annotation.Keep <fields>;
}
-keepclasseswithmembers,allowaccessmodification class * {
  @androidx.annotation.Keep <methods>;
}
# -------- Config Path: third_party/jni_zero/proguard.flags --------
# Copyright 2023 The Chromium Authors
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

# Keeps for method level annotations.
-keepclasseswithmembers,allowaccessmodification class ** {
  @org.jni_zero.AccessedByNative <fields>;
}
-keepclasseswithmembers,includedescriptorclasses,allowaccessmodification class ** {
  @org.jni_zero.CalledByNative <methods>;
}
-keepclasseswithmembers,includedescriptorclasses,allowaccessmodification class ** {
  @org.jni_zero.CalledByNativeUnchecked <methods>;
}
