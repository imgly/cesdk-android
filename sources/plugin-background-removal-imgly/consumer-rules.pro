# ONNX Runtime constructs and calls several ai.onnxruntime classes from JNI.
# Keep the Java API intact so release builds do not remove constructors or
# rename members that are only referenced from native code.
-keep,includedescriptorclasses class ai.onnxruntime.** { *; }
