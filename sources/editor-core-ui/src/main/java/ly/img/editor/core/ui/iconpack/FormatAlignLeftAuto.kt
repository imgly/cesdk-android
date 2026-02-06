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

val IconPack.FormatAlignLeftAuto: ImageVector
    get() {
        if (formatalignleftauto != null) {
            return formatalignleftauto!!
        }
        formatalignleftauto = Builder(
            name = "Formatalignleftauto",
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
                moveTo(3.0f, 21.0f)
                horizontalLineTo(12.0f)
                verticalLineTo(19.0f)
                horizontalLineTo(3.0f)
                verticalLineTo(21.0f)
                close()
                moveTo(3.0f, 17.0f)
                horizontalLineTo(14.0f)
                verticalLineTo(15.0f)
                horizontalLineTo(3.0f)
                verticalLineTo(17.0f)
                close()
                moveTo(3.0f, 13.0f)
                horizontalLineTo(16.0f)
                verticalLineTo(11.0f)
                horizontalLineTo(3.0f)
                verticalLineTo(13.0f)
                close()
                moveTo(3.0f, 9.0f)
                horizontalLineTo(13.0f)
                verticalLineTo(7.0f)
                horizontalLineTo(3.0f)
                verticalLineTo(9.0f)
                close()
                moveTo(3.0f, 5.0f)
                horizontalLineTo(16.0f)
                verticalLineTo(3.0f)
                horizontalLineTo(3.0f)
                verticalLineTo(5.0f)
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
                moveTo(19.3244f, 14.5f)
                curveTo(19.5244f, 14.5f, 19.7086f, 14.5581f, 19.8752f, 14.6748f)
                curveTo(20.0418f, 14.7915f, 20.1582f, 14.9417f, 20.2248f, 15.125f)
                lineTo(22.7746f, 22.2998f)
                curveTo(22.8746f, 22.5831f, 22.838f, 22.8546f, 22.6633f, 23.1133f)
                curveTo(22.4887f, 23.3719f, 22.2423f, 23.5006f, 21.925f, 23.5f)
                curveTo(21.7417f, 23.5f, 21.575f, 23.4459f, 21.425f, 23.3379f)
                curveTo(21.2751f, 23.23f, 21.1665f, 23.0842f, 21.0998f, 22.9004f)
                lineTo(20.5998f, 21.5f)
                horizontalLineTo(17.3996f)
                lineTo(16.8996f, 22.9004f)
                curveTo(16.833f, 23.0835f, 16.7243f, 23.2293f, 16.5744f, 23.3379f)
                curveTo(16.4245f, 23.4464f, 16.2577f, 23.5007f, 16.0744f, 23.5f)
                curveTo(15.7581f, 23.4999f, 15.5127f, 23.371f, 15.3381f, 23.1133f)
                curveTo(15.1634f, 22.8553f, 15.1255f, 22.5838f, 15.2248f, 22.2998f)
                lineTo(17.7746f, 15.125f)
                curveTo(17.8413f, 14.9417f, 17.9586f, 14.7915f, 18.1252f, 14.6748f)
                curveTo(18.2918f, 14.5583f, 18.4752f, 14.5f, 18.675f, 14.5f)
                horizontalLineTo(19.3244f)
                close()
                moveTo(17.8498f, 20.1504f)
                horizontalLineTo(20.1496f)
                lineTo(18.9992f, 16.5f)
                lineTo(17.8498f, 20.1504f)
                close()
            }
        }.build()
        return formatalignleftauto!!
    }

private var formatalignleftauto: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.FormatAlignLeftAuto.IconPreview()
