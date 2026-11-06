# Voice isolation plugin for the CE.SDK Android editor

The plugin removes music and background noise from the audio of a video or audio clip and keeps the voice. It adds an
"Isolate Voice" button to the inspector bar of a selected clip, and a call to clean a clip from your own code.

- `ly.img:plugin-voice-isolation` holds the button, the block call and the `VoiceIsolator` interface. It has no model and
  no ONNX Runtime.
- `ly.img:plugin-voice-isolation-imgly` adds IMG.LY's isolator, which runs the DeepFilterNet3 model on the device with
  ONNX Runtime. It adds about 27 MB to an arm64-v8a app, and about 10 MB to its download size.

## Add the plugin

```kotlin
implementation("ly.img:plugin-voice-isolation-imgly:<version>")
```

```kotlin
Editor(
    license = license,
    configuration = {
        EditorConfiguration
            .remember(::VideoConfigurationBuilder)
            .then(::IMGLYVoiceIsolationPlugin)
    },
    onClose = onClose,
)
```

The button cleans the selected clip:

- An audio clip gets the cleaned audio.
- A video clip moves the audio track that the editor plays into a new audio clip, as "Separate Audio" does, and this
  clip gets the cleaned audio.
- Stereo stays stereo.
- The clip shows its loading state while the model runs. The button cleans one clip at a time, and taps on other clips
  wait.
- One undo step brings the original audio back.

Move or remove the button with `inspectorBarModifier`.

## The model

By default, the plugin downloads the model (8.6 MB) when the editor starts. Set `loadMode` to
`IMGLYVoiceIsolationConfig.LoadMode.LAZY` to download it on the first use instead.

`modelBaseUri` points to a folder with the files `enc.onnx`, `erb_dec.onnx`, `df_dec.onnx` and `config.ini`. To bundle
the model with your app, put the files into `src/main/assets/DeepFilterNet3` and set:

```kotlin
config = IMGLYVoiceIsolationConfig(modelBaseUri = "file:///android_asset/DeepFilterNet3".toUri())
```

DeepFilterNet3 is by Hendrik Schröter and is licensed under MIT or Apache-2.0
([Rikorose/DeepFilterNet](https://github.com/Rikorose/DeepFilterNet)).

## Clean a clip from your code

`isolateVoice` cleans a clip as the button does, wherever your app has the engine, for example in `onCreate` after the
scene exists. Check a clip with `canIsolateVoice` first:

```kotlin
if (canIsolateVoice(block = block, engine = engine)) {
    val cleanedClip = isolateVoice(block = block, engine = engine, config = config, context = context)
}
```

Unlike the button, the call leaves three things to your app:

- It does not wait for other calls. The isolator decides how many of them it runs at once.
- It shows no loading state. Set the clip's state to `BlockState.Pending` before the call and back to
  `BlockState.Ready` when it returns or throws.
- It adds no undo step, so the change joins the next undo step of the editor. Call `engine.editor.addUndoStep()`
  afterwards to make the change an undo step of its own.

The call throws a `VoiceIsolationException`: `UnsupportedBlock`, `NoAudio`, `BlockChanged` when the clip
changes or is deleted during the run, `ModelDownloadFailed` or `ProcessingFailed`.

## Keep the cleaned audio

The cleaned audio is a WAV file (16-bit, 48 kHz, the channels of the clip) in the app's files directory, under
`ly.img.plugin.voiceIsolation`. The plugin never deletes them. A scene that you save refers to these files, so its
cleaned audio plays only on this device. To keep the audio elsewhere, upload each file when the editor exports or
closes, point the scene to the upload, and delete the local file:

```kotlin
engine.editor.relocateResource(currentUri = localUri, relocatedUri = uploadedUri)
```

## Use your own model or service

Implement `VoiceIsolator` and pass it in your own `VoiceIsolationConfig` to `VoiceIsolationPlugin`. The plugin decodes
the clip's audio into a WAV file (16-bit, 48 kHz, the channels of the clip), and your isolator writes the cleaned audio
to the output file in the same format.

```kotlin
class MyVoiceIsolator : VoiceIsolator {
    override suspend fun isolateVoice(context: Context, input: File, output: File) {
        // Clean input and write the result to output.
    }
}

EditorConfiguration
    .remember(::VideoConfigurationBuilder)
    .then(::VoiceIsolationPlugin) {
        config = object : VoiceIsolationConfig {
            override val httpClient = OkHttpClient()
            override val isolator = MyVoiceIsolator()
        }
    }
```

To run your model before IMG.LY's, write its result to a file in the input format (16-bit, 48 kHz WAV) and pass that
file to `IMGLYVoiceIsolator(IMGLYVoiceIsolationConfig()).isolateVoice`.
