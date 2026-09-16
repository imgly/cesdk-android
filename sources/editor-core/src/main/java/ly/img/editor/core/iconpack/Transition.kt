package ly.img.editor.core.iconpack

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.EvenOdd
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

val IconPack.Transition: ImageVector
    get() {
        if (transition != null) {
            return transition!!
        }
        transition = Builder(
            name = "Transition",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFF46464F)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = EvenOdd,
            ) {
                moveTo(18.671f, 4.732f)
                curveTo(19.961f, 3.586f, 22.0f, 4.501f, 22.0f, 6.226f)
                verticalLineTo(17.773f)
                curveTo(22.0f, 19.499f, 19.961f, 20.414f, 18.671f, 19.267f)
                lineTo(12.177f, 13.495f)
                curveTo(12.113f, 13.438f, 12.055f, 13.377f, 12.0f, 13.315f)
                curveTo(11.945f, 13.377f, 11.887f, 13.438f, 11.823f, 13.495f)
                lineTo(5.329f, 19.267f)
                curveTo(4.039f, 20.414f, 2.0f, 19.499f, 2.0f, 17.773f)
                verticalLineTo(6.226f)
                curveTo(2.001f, 4.501f, 4.039f, 3.586f, 5.329f, 4.732f)
                lineTo(11.823f, 10.505f)
                curveTo(11.887f, 10.561f, 11.945f, 10.622f, 12.0f, 10.684f)
                curveTo(12.055f, 10.622f, 12.113f, 10.561f, 12.177f, 10.505f)
                lineTo(18.671f, 4.732f)
                close()
                moveTo(13.505f, 12.0f)
                lineTo(20.0f, 17.773f)
                verticalLineTo(6.226f)
                lineTo(13.505f, 12.0f)
                close()
            }
        }
            .build()
        return transition!!
    }

private var transition: ImageVector? = null

@Preview
@Composable
private fun Preview() {
    Box(modifier = Modifier.padding(12.dp)) {
        Image(imageVector = IconPack.Transition, contentDescription = "")
    }
}
