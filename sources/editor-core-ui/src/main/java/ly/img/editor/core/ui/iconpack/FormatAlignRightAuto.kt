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

val IconPack.FormatAlignRightAuto: ImageVector
    get() {
        if (formatalignrightauto != null) {
            return formatalignrightauto!!
        }
        formatalignrightauto = Builder(
            name = "Formatalignrightauto",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFF49454F)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(5.3244f, 14.5f)
                curveTo(5.5244f, 14.5f, 5.7086f, 14.5581f, 5.8752f, 14.6748f)
                curveTo(6.0418f, 14.7915f, 6.1582f, 14.9417f, 6.2248f, 15.125f)
                lineTo(8.7746f, 22.2998f)
                curveTo(8.8746f, 22.5831f, 8.838f, 22.8546f, 8.6633f, 23.1133f)
                curveTo(8.4887f, 23.3719f, 8.2423f, 23.5006f, 7.925f, 23.5f)
                curveTo(7.7417f, 23.5f, 7.575f, 23.4459f, 7.425f, 23.3379f)
                curveTo(7.2751f, 23.23f, 7.1665f, 23.0842f, 7.0998f, 22.9004f)
                lineTo(6.5998f, 21.5f)
                horizontalLineTo(3.3996f)
                lineTo(2.8996f, 22.9004f)
                curveTo(2.833f, 23.0835f, 2.7243f, 23.2293f, 2.5744f, 23.3379f)
                curveTo(2.4245f, 23.4464f, 2.2577f, 23.5007f, 2.0744f, 23.5f)
                curveTo(1.7581f, 23.4999f, 1.5127f, 23.371f, 1.3381f, 23.1133f)
                curveTo(1.1634f, 22.8553f, 1.1255f, 22.5838f, 1.2248f, 22.2998f)
                lineTo(3.7746f, 15.125f)
                curveTo(3.8413f, 14.9417f, 3.9586f, 14.7915f, 4.1252f, 14.6748f)
                curveTo(4.2918f, 14.5583f, 4.4752f, 14.5f, 4.675f, 14.5f)
                horizontalLineTo(5.3244f)
                close()
                moveTo(3.8498f, 20.1504f)
                horizontalLineTo(6.1496f)
                lineTo(4.9992f, 16.5f)
                lineTo(3.8498f, 20.1504f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF49454F)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(21.0f, 21.0f)
                horizontalLineTo(12.0f)
                verticalLineTo(19.0f)
                horizontalLineTo(21.0f)
                verticalLineTo(21.0f)
                close()
                moveTo(21.0f, 17.0f)
                horizontalLineTo(10.0f)
                verticalLineTo(15.0f)
                horizontalLineTo(21.0f)
                verticalLineTo(17.0f)
                close()
                moveTo(21.0f, 13.0f)
                horizontalLineTo(8.0f)
                verticalLineTo(11.0f)
                horizontalLineTo(21.0f)
                verticalLineTo(13.0f)
                close()
                moveTo(21.0f, 9.0f)
                horizontalLineTo(11.0f)
                verticalLineTo(7.0f)
                horizontalLineTo(21.0f)
                verticalLineTo(9.0f)
                close()
                moveTo(21.0f, 5.0f)
                horizontalLineTo(8.0f)
                verticalLineTo(3.0f)
                horizontalLineTo(21.0f)
                verticalLineTo(5.0f)
                close()
            }
        }.build()
        return formatalignrightauto!!
    }

private var formatalignrightauto: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.FormatAlignRightAuto.IconPreview()
