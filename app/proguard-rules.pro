# Leaf R8 rules (M8). Release builds shrink with proguard-android-optimize.txt plus this file.
#
# Room, Compose, Koin and AndroidX ship their own consumer rules, so nothing is listed for them.
# Readium and PDFBox-Android load classes and resources in ways R8 cannot see statically, so
# their packages are kept whole. This is conservative: it trades a little APK size for not
# breaking reflection-based loading. Narrow it once the release build is verified on device.
-keep class org.readium.** { *; }
-dontwarn org.readium.**
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.**

# Keep line numbers so release stack traces map back through mapping.txt.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
