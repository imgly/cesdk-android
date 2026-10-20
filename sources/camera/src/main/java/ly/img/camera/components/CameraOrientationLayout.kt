package ly.img.camera.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints

/** Maps the normalized rotation (0, 90, 180, or 270 degrees) to an alignment in the parent. */
@Composable
internal fun BoxScope.CameraOrientationLayout(
    orientation: Int,
    rotation: () -> Float,
    alignmentForRotation: (Int) -> Alignment,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val targetPosition = CameraPlacement(
        alignment = alignmentForRotation(orientation),
        sideways = orientation == 90 || orientation == 270,
    )
    val positionTransition = rememberCameraPositionTransition(targetPosition, orientation)
    CameraRotatedLayout(
        modifier = modifier.align(positionTransition.position.alignment),
        sideways = positionTransition.position.sideways,
        rotation = { positionTransition.rotationFor(targetPosition, rotation) },
        alpha = { positionTransition.alpha.value },
        content = content,
    )
}

@Composable
internal fun CameraRotatedLayout(
    sideways: Boolean,
    rotation: () -> Float,
    alpha: () -> Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.cameraRotation(sideways, rotation, alpha)) { content() }
}

internal fun Modifier.cameraRotation(
    sideways: Boolean,
    rotation: () -> Float,
    alpha: () -> Float,
): Modifier = layout { measurable, constraints ->
    val contentConstraints = if (sideways) {
        Constraints(
            minWidth = constraints.minHeight,
            maxWidth = constraints.maxHeight,
            minHeight = constraints.minWidth,
            maxHeight = constraints.maxWidth,
        )
    } else {
        constraints
    }
    val placeable = measurable.measure(contentConstraints)
    val width = if (sideways) placeable.height else placeable.width
    val height = if (sideways) placeable.width else placeable.height
    layout(width, height) {
        placeable.placeWithLayer(
            x = (width - placeable.width) / 2,
            y = (height - placeable.height) / 2,
        ) {
            rotationZ = rotation()
            this.alpha = alpha()
        }
    }
}

private data class CameraPlacement(
    val alignment: Alignment,
    val sideways: Boolean,
)
