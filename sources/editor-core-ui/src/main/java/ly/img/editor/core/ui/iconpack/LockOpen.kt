package ly.img.editor.core.ui.iconpack

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

val IconPack.LockOpen: ImageVector
    get() {
        if (lockOpen != null) {
            return lockOpen!!
        }
        lockOpen = Builder(
            name = "LockOpen",
            defaultWidth = 16.0.dp,
            defaultHeight = 21.0.dp,
            viewportWidth = 16.0f,
            viewportHeight = 21.0f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFF46464F)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(2.0f, 7.0f)
                horizontalLineTo(11.0f)
                verticalLineTo(5.0f)
                curveTo(11.0f, 4.1667f, 10.7083f, 3.4583f, 10.125f, 2.875f)
                curveTo(9.5417f, 2.2917f, 8.8333f, 2.0f, 8.0f, 2.0f)
                curveTo(7.1667f, 2.0f, 6.4583f, 2.2917f, 5.875f, 2.875f)
                curveTo(5.2917f, 3.4583f, 5.0f, 4.1667f, 5.0f, 5.0f)
                horizontalLineTo(3.0f)
                curveTo(3.0f, 3.6167f, 3.4877f, 2.4373f, 4.463f, 1.462f)
                curveTo(5.4377f, 0.4873f, 6.6167f, 0.0f, 8.0f, 0.0f)
                curveTo(9.3833f, 0.0f, 10.5627f, 0.4873f, 11.538f, 1.462f)
                curveTo(12.5127f, 2.4373f, 13.0f, 3.6167f, 13.0f, 5.0f)
                verticalLineTo(7.0f)
                horizontalLineTo(14.0f)
                curveTo(14.55f, 7.0f, 15.021f, 7.1957f, 15.413f, 7.587f)
                curveTo(15.8043f, 7.979f, 16.0f, 8.45f, 16.0f, 9.0f)
                verticalLineTo(19.0f)
                curveTo(16.0f, 19.55f, 15.8043f, 20.021f, 15.413f, 20.413f)
                curveTo(15.021f, 20.8043f, 14.55f, 21.0f, 14.0f, 21.0f)
                horizontalLineTo(2.0f)
                curveTo(1.45f, 21.0f, 0.9793f, 20.8043f, 0.588f, 20.413f)
                curveTo(0.196f, 20.021f, 0.0f, 19.55f, 0.0f, 19.0f)
                verticalLineTo(9.0f)
                curveTo(0.0f, 8.45f, 0.196f, 7.979f, 0.588f, 7.587f)
                curveTo(0.9793f, 7.1957f, 1.45f, 7.0f, 2.0f, 7.0f)
                close()
                moveTo(2.0f, 19.0f)
                horizontalLineTo(14.0f)
                verticalLineTo(9.0f)
                horizontalLineTo(2.0f)
                verticalLineTo(19.0f)
                close()
                moveTo(8.0f, 16.0f)
                curveTo(8.55f, 16.0f, 9.021f, 15.8043f, 9.413f, 15.413f)
                curveTo(9.8043f, 15.021f, 10.0f, 14.55f, 10.0f, 14.0f)
                curveTo(10.0f, 13.45f, 9.8043f, 12.979f, 9.413f, 12.587f)
                curveTo(9.021f, 12.1957f, 8.55f, 12.0f, 8.0f, 12.0f)
                curveTo(7.45f, 12.0f, 6.9793f, 12.1957f, 6.588f, 12.587f)
                curveTo(6.196f, 12.979f, 6.0f, 13.45f, 6.0f, 14.0f)
                curveTo(6.0f, 14.55f, 6.196f, 15.021f, 6.588f, 15.413f)
                curveTo(6.9793f, 15.8043f, 7.45f, 16.0f, 8.0f, 16.0f)
                close()
                moveTo(2.0f, 19.0f)
                verticalLineTo(9.0f)
                verticalLineTo(19.0f)
                close()
            }
        }
            .build()
        return lockOpen!!
    }

private var lockOpen: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.LockOpen.IconPreview()
