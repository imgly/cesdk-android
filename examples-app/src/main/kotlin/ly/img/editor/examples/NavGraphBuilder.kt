package ly.img.editor.examples

import AIImageGenerationEditorSolution
import ActionsEditorSolution
import AddButtonEditorSolution
import AutomationActionsEditorSolution
import BackgroundRemovalEditorSolution
import BuildYourOwnUIScreen
import CallbacksEditorSolution
import ColorPaletteEditorSolution
import CropPresetsEditorSolution
import CustomFeaturePluginEditorSolution
import CustomFontsEditorSolution
import CustomPanelSolution
import CustomizeBehaviourEditorSolution
import DataMergeGuideScreen
import DesignUnitsScreen
import EditorStateEditorSolution
import ForceCropEditorSolution
import ForceTrimVideoSolution
import HideElementsEditorSolution
import IconsEditorSolution
import ModifyingScenesEngineSolution
import NewListBuilderDockSolution
import OverlayEditorSolution
import PageFormatEditorSolution
import RearrangeButtonsEditorSolution
import RecordVoiceoverSolution
import TemplatingEditorSolution
import ThemingEditorSolution
import UiEventsEditorSolution
import UserInterfaceAssetLibraryEditorSolution
import android.net.Uri
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import ly.img.editor.Editor
import ly.img.editor.configuration.apparel.ApparelConfigurationBuilder
import ly.img.editor.configuration.design.DesignConfigurationBuilder
import ly.img.editor.configuration.photo.PhotoConfigurationBuilder
import ly.img.editor.configuration.postcard.PostcardConfigurationBuilder
import ly.img.editor.configuration.video.VideoConfigurationBuilder
import ly.img.editor.core.configuration.EditorConfiguration
import ly.img.editor.core.configuration.remember

fun NavGraphBuilder.build(navController: NavHostController) {
    composable(destination = Destination.Home) {
        HomeScreen(onClick = { navController.navigate(it.route) })
    }
    composable(destination = Destination.PhotoEditor) {
        Editor(
            license = Secrets.license,
            configuration = { EditorConfiguration.remember(::PhotoConfigurationBuilder) },
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.VideoEditor) {
        Editor(
            license = Secrets.license,
            configuration = { EditorConfiguration.remember(::VideoConfigurationBuilder) },
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.DesignEditor) {
        Editor(
            license = Secrets.license,
            configuration = { EditorConfiguration.remember(::DesignConfigurationBuilder) },
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.PostcardEditor) {
        Editor(
            license = Secrets.license,
            configuration = { EditorConfiguration.remember(::PostcardConfigurationBuilder) },
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.ApparelEditor) {
        Editor(
            license = Secrets.license,
            configuration = { EditorConfiguration.remember(::ApparelConfigurationBuilder) },
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.AutoResizeGuide) {
        AutoResizeGuideScreen(
            license = Secrets.license,
        )
    }
    composable(destination = Destination.DataMerge) {
        DataMergeGuideScreen(
            navController = navController,
            license = Secrets.license,
        )
    }
    composable(destination = Destination.GuideColorPalette) {
        ColorPaletteEditorSolution(
            license = Secrets.license,
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.GuideCustomFonts) {
        CustomFontsEditorSolution(
            license = Secrets.license,
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.GuideCallbacks) {
        CallbacksEditorSolution(
            license = Secrets.license,
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.GuideUiEvents) {
        UiEventsEditorSolution(
            license = Secrets.license,
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.GuideForceCrop) {
        ForceCropEditorSolution(
            license = Secrets.license,
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.GuideDock) {
        NewListBuilderDockSolution(
            license = Secrets.license,
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.GuideCropPresets) {
        CropPresetsEditorSolution(
            license = Secrets.license,
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.GuidePageFormat) {
        PageFormatEditorSolution(
            license = Secrets.license,
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.GuideCustomPanel) {
        CustomPanelSolution(
            license = Secrets.license,
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.GuideTheming) {
        ThemingEditorSolution(
            license = Secrets.license,
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.GuideIcons) {
        IconsEditorSolution(
            license = Secrets.license,
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.GuideOverlay) {
        OverlayEditorSolution(
            license = Secrets.license,
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.GuideHideElements) {
        HideElementsEditorSolution(
            license = Secrets.license,
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.GuideForceTrim) {
        ForceTrimVideoSolution(
            license = Secrets.license,
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.GuideRecordVoiceover) {
        RecordVoiceoverSolution(
            license = Secrets.license,
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.AutomateWorkflows) {
        AutomateWorkflowsScreen(
            license = Secrets.license,
        )
    }
    composable(destination = Destination.GuideEditorState) {
        EditorStateEditorSolution(
            license = Secrets.license,
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.GuideTemplating) {
        TemplatingEditorSolution(
            license = Secrets.license,
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.GuideDesignUnits) {
        DesignUnitsScreen(license = Secrets.license)
    }
    composable(destination = Destination.GuideScenes) {
        ModifyingScenesEngineSolution(
            license = Secrets.license,
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.GuideBuildYourOwnUI) {
        BuildYourOwnUIScreen(
            license = Secrets.license,
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.GuideActions) {
        ActionsEditorSolution(
            license = Secrets.license,
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.GuideAutomationActions) {
        AutomationActionsEditorSolution(
            license = Secrets.license,
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.GuideCustomizeBehaviour) {
        CustomizeBehaviourEditorSolution(
            license = Secrets.license,
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.GuideRearrangeButtons) {
        RearrangeButtonsEditorSolution(
            license = Secrets.license,
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.GuideAddButton) {
        AddButtonEditorSolution(
            license = Secrets.license,
            baseUri = Uri.parse(ExamplesBuildConfig.GUIDE_ADD_BUTTON_BASE_URI),
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.GuideCustomFeaturePlugin) {
        CustomFeaturePluginEditorSolution(
            license = Secrets.license,
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.GuideBackgroundRemoval) {
        BackgroundRemovalEditorSolution(
            license = Secrets.license,
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.GuideAIImageGeneration) {
        AIImageGenerationEditorSolution(
            license = Secrets.license,
            aiGatewayApiKey = Secrets.gatewayApiKey,
            onClose = { navController.popBackStack() },
        )
    }
    composable(destination = Destination.GuideAssetLibrary) {
        UserInterfaceAssetLibraryEditorSolution(
            license = Secrets.license,
            onClose = { navController.popBackStack() },
        )
    }
}

private fun NavGraphBuilder.composable(
    destination: Destination,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable(
        route = destination.route,
        deepLinks = destination.deeplinkSchemes.map { uriPattern ->
            navDeepLink { this.uriPattern = uriPattern }
        },
        content = content,
    )
}
