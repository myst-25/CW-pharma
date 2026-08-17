# libxposed API 102 recommended rules (see https://github.com/libxposed/api)
-dontwarn io.github.libxposed.annotation.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}

# Entry class is registered via `libxposed_init` manifest metadata as a literal
# class name, so it must NOT be obfuscated or removed.
-keep public class com.cwpdf.saver.MainHook {
    public <init>();
}
