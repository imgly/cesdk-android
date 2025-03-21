package ly.img.editor.core.iconpack

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

val IconPack.ArrowForward: ImageVector
    get() {
        if (arrowForward != null) {
            return arrowForward!!
        }
        arrowForward =
            Builder(
                name = "Icon",
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
                    pathFillType = NonZero,
                ) {
                    moveTo(16.175f, 13.0f)
                    horizontalLineTo(4.0f)
                    verticalLineTo(11.0f)
                    horizontalLineTo(16.175f)
                    lineTo(10.575f, 5.4f)
                    lineTo(12.0f, 4.0f)
                    lineTo(20.0f, 12.0f)
                    lineTo(12.0f, 20.0f)
                    lineTo(10.575f, 18.6f)
                    lineTo(16.175f, 13.0f)
                    close()
                }
            }
                .build()
        return arrowForward!!
    }

private var arrowForward: ImageVector? = null

@Preview
@Composable
private fun Preview() {
    Box(modifier = Modifier.padding(12.dp)) {
        Image(imageVector = IconPack.ArrowForward, contentDescription = "")
    }
}
