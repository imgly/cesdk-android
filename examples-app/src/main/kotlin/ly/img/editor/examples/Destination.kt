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

    data object AutoResizeGuide : Destination(
        route = "guide-auto-resize",
        title = "Auto-Resize",
    )

    data object DataMerge : Destination(
        route = "guides-data-merge",
        title = "Data Merge",
    )

    data object GuideColorPalette : Destination(
        route = "guide-color-palette",
        title = "Color Palette",
    )

    data object GuideCustomFonts : Destination(
        route = "guide-custom-fonts",
        title = "Custom Fonts",
    )

    data object GuideCallbacks : Destination(
        route = "guide-callbacks",
        title = "Callbacks",
    )

    data object GuideUiEvents : Destination(
        route = "guide-ui-events",
        title = "UI Events",
    )

    data object GuideForceCrop : Destination(
        route = "guide-force-crop",
        title = "Force Crop",
    )

    data object GuideDock : Destination(
        route = "guide-dock",
        title = "Dock",
    )

    data object GuideCropPresets : Destination(
        route = "guide-crop-presets",
        title = "Crop Presets",
    )

    data object GuidePageFormat : Destination(
        route = "guide-page-format",
        title = "Page Format",
    )

    data object GuideCustomPanel : Destination(
        route = "guide-custom-panel",
        title = "Custom Panel",
    )

    data object GuideTheming : Destination(
        route = "guide-theming",
        title = "Theming",
    )

    data object GuideIcons : Destination(
        route = "guide-icons",
        title = "Icons",
    )

    data object GuideOverlay : Destination(
        route = "guide-overlay",
        title = "Overlay",
    )

    data object GuideHideElements : Destination(
        route = "guide-hide-elements",
        title = "Hide Elements",
    )

    data object GuideForceTrim : Destination(
        route = "guide-force-trim",
        title = "Force Trim",
    )

    data object GuideRecordVoiceover : Destination(
        route = "guide-record-voiceover",
        title = "Record Voiceover",
    )

    data object AutomateWorkflows : Destination(
        route = "guide-automate-workflows",
        title = "Automate Workflows",
    )

    data object GuideEditorState : Destination(
        route = "guide-editor-state",
        title = "Editor State",
    )

    data object GuideTemplating : Destination(
        route = "guide-templating",
        title = "Templating",
    )

    data object GuideDesignUnits : Destination(
        route = "guide-design-units",
        title = "Design Units",
    )

    data object GuideScenes : Destination(
        route = "guide-scenes",
        title = "Scenes",
    )

    data object GuideBuildYourOwnUI : Destination(
        route = "guide-build-your-own-ui",
        title = "Build Your Own UI",
    )

    data object GuideActions : Destination(
        route = "guide-actions",
        title = "Actions",
    )

    data object GuideAutomationActions : Destination(
        route = "guide-automation-actions",
        title = "Automation Actions",
    )

    data object GuideCustomizeBehaviour : Destination(
        route = "guide-customize-behaviour",
        title = "Customize Behaviour",
    )

    data object GuideRearrangeButtons : Destination(
        route = "guide-rearrange-buttons",
        title = "Rearrange Buttons",
    )

    data object GuideAddButton : Destination(
        route = "guide-add-button",
        title = "Add a New Button",
    )

    data object GuideCustomFeaturePlugin : Destination(
        route = "guide-custom-feature-plugin",
        title = "Custom Feature Plugin",
    )

    data object GuideBackgroundRemoval : Destination(
        route = "guide-background-removal",
        title = "Background Removal",
    )

    data object GuideAIImageGeneration : Destination(
        route = "guide-ai-image-generation",
        title = "AI Image Generation",
    )

    data object GuideAssetLibrary : Destination(
        route = "guide-asset-library",
        title = "Asset Library",
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
            AutoResizeGuide,
            DataMerge,
            GuideColorPalette,
            GuideCustomFonts,
            GuideCallbacks,
            GuideUiEvents,
            GuideForceCrop,
            GuideDock,
            GuideCropPresets,
            GuidePageFormat,
            GuideCustomPanel,
            GuideTheming,
            GuideIcons,
            GuideOverlay,
            GuideHideElements,
            GuideForceTrim,
            GuideRecordVoiceover,
            AutomateWorkflows,
            GuideEditorState,
            GuideTemplating,
            GuideDesignUnits,
            GuideScenes,
            GuideBuildYourOwnUI,
            GuideActions,
            GuideAutomationActions,
            GuideCustomizeBehaviour,
            GuideRearrangeButtons,
            GuideAddButton,
            GuideCustomFeaturePlugin,
            GuideBackgroundRemoval,
            GuideAIImageGeneration,
            GuideAssetLibrary,
        )
    }
}
