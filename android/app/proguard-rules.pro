# Vosk talks to its native library through JNA, which finds classes and fields by name at run time.
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
-keep class org.vosk.** { *; }
-dontwarn java.awt.**

# JNI entry points in lucia_llama.cpp.
-keep class com.zirakusa.gatewayhub.assistant.local.NativeLlama { native <methods>; }
