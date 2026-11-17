# ONNX Runtime constructs and calls several ai.onnxruntime classes from JNI.
# Keep the Java API intact so release builds do not remove constructors or
# rename members that are only referenced from native code.
-keep,includedescriptorclasses class ai.onnxruntime.** { *; }

# JLargeArrays, which JTransforms uses, refers to sun.misc.Cleaner, which Android does not have.
# Without this rule, R8 stops on the missing class in every minified app.
-dontwarn sun.misc.Cleaner
