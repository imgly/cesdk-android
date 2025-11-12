![Hero image showing the configuration abilities of IMGLY editor](https://img.ly/static/cesdk_release_header_android.png)

# IMG.LY Editor and Camera

This repository contains the Android version of the IMG.LY UI for the _Creative Engine_, the UI for CE.SDK.
The Creative Engine enables you to build any design editing UI, automation and creative workflow in Kotlin.
It offers performant and robust graphics processing capabilities combining the best of layout, typography and image processing with advanced workflows centered around templating and adaptation.

The Creative Engine seamlessly integrates into any iOS app whether you are building a photo editor, template-based design tool or scalable automation of content creation for your app.

Visit our [documentation](https://img.ly/docs/cesdk) for more tutorials on how to integrate and
customize the engine for your specific use case.

## License

The CreativeEditor SDK is a commercial product. To use it and get access you need to unlock the SDK with a license file. You can purchase a license at https://img.ly/pricing.

## Integration

### Jetpack Compose

```Kotlin
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import ly.img.editor.DesignEditor
import ly.img.editor.EditorConfiguration
import ly.img.editor.EngineConfiguration
import ly.img.editor.rememberForDesign

// Add this composable to your NavHost
@Composable
fun EditorIntegration(navController: NavHostController) {
    val engineConfiguration =
        EngineConfiguration.rememberForDesign(
            license = "<your license here>",
            userId = "<your unique user id>",
        )
    val editorConfiguration = EditorConfiguration.rememberForDesign()
    // Also available PhotoEditor, VideoEditor, PostcardEditor and CaptureVideo contract for camera
    DesignEditor(
        engineConfiguration = engineConfiguration,
        editorConfiguration = editorConfiguration,
    ) {
        // You can set result here
        navController.popBackStack()
    }
}
```

## Documentation

The repository contains source code of the [mobile editor](https://img.ly/docs/cesdk/mobile-editor/quickstart?platform=android) and the [mobile camera](https://img.ly/docs/cesdk/mobile-camera/quickstart?platform=android). The full documentation can be found on our website.
There you will learn how to integrate and configure them for your use case.

## Changelog

To keep up-to-date with the latest changes, visit [CHANGELOG](https://img.ly/docs/cesdk/changelog/).
