# IMG.LY voice isolation for the CE.SDK Android editor

This module runs the DeepFilterNet3 model on the device with ONNX Runtime for the voice isolation plugin. Its
`IMGLYVoiceIsolationPlugin` is the plugin with IMG.LY's isolator, and `IMGLYVoiceIsolationConfig` sets where the model
comes from and when it is downloaded.

See [`plugin-voice-isolation`](../plugin-voice-isolation/README.md) for the setup, the block call and the result files.

DeepFilterNet3 is by Hendrik Schröter and is licensed under MIT or Apache-2.0
([Rikorose/DeepFilterNet](https://github.com/Rikorose/DeepFilterNet)). The module also uses JTransforms (BSD-2-Clause)
for the FFT.
