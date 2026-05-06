package ly.img.editor.examples

sealed class Destination(
    val route: String,
    val title: String,
) {
    val deeplinkSchemes = listOf(
        "imgly://examples/$route",
    )

    data object Home : Destination(
        route = "home",
        title = "Home",
    )

    data object PhotoEditor : Destination(
        route = "starter-kit-photo-editor",
        title = "Photo Editor",
    )

    data object VideoEditor : Destination(
        route = "starter-kit-video-editor",
        title = "Video Editor",
    )

    data object DesignEditor : Destination(
        route = "starter-kit-design-editor",
        title = "Design Editor",
    )

    data object PostcardEditor : Destination(
        route = "starter-kit-postcard-editor",
        title = "Postcard Editor",
    )

    data object ApparelEditor : Destination(
        route = "starter-kit-apparel-editor",
        title = "Apparel Editor",
    )

    data object GuideColorPalette : Destination(
        route = "guide-color-palette",
        title = "Color Palette",
    )

    data object GuideCallbacks : Destination(
        route = "guide-callbacks",
        title = "Callbacks",
    )

    data object GuideForceCrop : Destination(
        route = "guide-force-crop",
        title = "Force Crop",
    )

    data object GuideCustomPanel : Destination(
        route = "guide-custom-panel",
        title = "Custom Panel",
    )

    data object GuideTheming : Destination(
        route = "guide-theming",
        title = "Theming",
    )

    data object GuideOverlay : Destination(
        route = "guide-overlay",
        title = "Overlay",
    )

    data object GuideForceTrim : Destination(
        route = "guide-force-trim",
        title = "Force Trim",
    )

    companion object {
        val starterKits = listOf(
            PhotoEditor,
            VideoEditor,
            DesignEditor,
            PostcardEditor,
            ApparelEditor,
        )

        val guides = listOf(
            GuideColorPalette,
            GuideCallbacks,
            GuideForceCrop,
            GuideCustomPanel,
            GuideTheming,
            GuideOverlay,
            GuideForceTrim,
        )
    }
}
