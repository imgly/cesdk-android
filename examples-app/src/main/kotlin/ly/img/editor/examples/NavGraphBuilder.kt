package ly.img.editor.examples

import CallbacksEditorSolution
import ColorPaletteEditorSolution
import CustomPanelSolution
import ForceCropEditorSolution
import ForceTrimVideoSolution
import OverlayEditorSolution
import ThemingEditorSolution
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
    composable(destination = Destination.GuideColorPalette) {
        ColorPaletteEditorSolution(
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
    composable(destination = Destination.GuideForceCrop) {
        ForceCropEditorSolution(
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
    composable(destination = Destination.GuideOverlay) {
        OverlayEditorSolution(
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
