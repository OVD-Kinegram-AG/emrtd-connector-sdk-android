-keep class com.kinegram.android.** { *; }

-dontshrink

# Needed because we use enums, see https://web.archive.org/web/20250520233313/https://www.guardsquare.com/manual/configuration/examples#enumerations
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepattributes InnerClasses # Otherwise AccessKey.FromXyz will not work
