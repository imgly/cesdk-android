package ly.img.editor.core.iconpack

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.EvenOdd
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

val IconPack.Animation: ImageVector
    get() {
        if (animation != null) {
            return animation!!
        }
        animation = Builder(
            name = "Animation",
            defaultWidth = 24.0.dp,
            defaultHeight =
                24.0.dp,
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
                moveTo(16.766f, 7.449f)
                curveTo(16.486f, 6.973f, 15.874f, 6.813f, 15.397f, 7.093f)
                curveTo(14.921f, 7.373f, 14.762f, 7.985f, 15.041f, 8.462f)
                curveTo(15.056f, 8.487f, 15.071f, 8.513f, 15.086f, 8.539f)
                curveTo(15.36f, 9.019f, 15.97f, 9.186f, 16.45f, 8.912f)
                curveTo(16.93f, 8.639f, 17.097f, 8.028f, 16.824f, 7.548f)
                curveTo(16.805f, 7.515f, 16.785f, 7.482f, 16.766f, 7.449f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF46464F)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(18.0f, 11.942f)
                curveTo(17.996f, 11.39f, 17.546f, 10.945f, 16.994f, 10.949f)
                curveTo(16.441f, 10.952f, 15.996f, 11.403f, 16.0f, 11.955f)
                lineTo(16.0f, 12.0f)
                lineTo(16.0f, 12.045f)
                curveTo(15.996f, 12.597f, 16.441f, 13.048f, 16.994f, 13.051f)
                curveTo(17.546f, 13.055f, 17.996f, 12.61f, 18.0f, 12.058f)
                lineTo(18.0f, 12.0f)
                lineTo(18.0f, 11.942f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF46464F)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(16.824f, 16.452f)
                curveTo(17.097f, 15.972f, 16.93f, 15.361f, 16.45f, 15.088f)
                curveTo(15.97f, 14.814f, 15.36f, 14.981f, 15.086f, 15.461f)
                curveTo(15.071f, 15.487f, 15.056f, 15.513f, 15.041f, 15.538f)
                curveTo(14.762f, 16.015f, 14.921f, 16.628f, 15.397f, 16.907f)
                curveTo(15.874f, 17.187f, 16.486f, 17.027f, 16.766f, 16.551f)
                curveTo(16.785f, 16.518f, 16.805f, 16.485f, 16.824f, 16.452f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF46464F)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = EvenOdd,
            ) {
                moveTo(9.0f, 3.0f)
                lineTo(8.942f, 3.0f)
                curveTo(8.39f, 3.004f, 7.945f, 3.454f, 7.949f, 4.006f)
                curveTo(7.952f, 4.559f, 8.403f, 5.004f, 8.955f, 5.0f)
                lineTo(9.0f, 5.0f)
                lineTo(9.045f, 5.0f)
                curveTo(9.186f, 5.001f, 9.32f, 4.973f, 9.441f, 4.921f)
                curveTo(7.346f, 6.569f, 6.0f, 9.127f, 6.0f, 12.0f)
                curveTo(6.0f, 14.873f, 7.346f, 17.431f, 9.441f, 19.079f)
                curveTo(9.32f, 19.027f, 9.186f, 18.999f, 9.045f, 19.0f)
                lineTo(9.0f, 19.0f)
                lineTo(8.955f, 19.0f)
                curveTo(8.403f, 18.996f, 7.952f, 19.441f, 7.949f, 19.994f)
                curveTo(7.945f, 20.546f, 8.39f, 20.996f, 8.942f, 21.0f)
                lineTo(9.0f, 21.0f)
                lineTo(9.058f, 21.0f)
                curveTo(9.61f, 20.996f, 10.055f, 20.546f, 10.051f, 19.994f)
                curveTo(10.05f, 19.753f, 9.964f, 19.533f, 9.821f, 19.362f)
                curveTo(11.286f, 20.394f, 13.072f, 21.0f, 15.0f, 21.0f)
                curveTo(19.971f, 21.0f, 24.0f, 16.971f, 24.0f, 12.0f)
                curveTo(24.0f, 7.029f, 19.971f, 3.0f, 15.0f, 3.0f)
                curveTo(13.072f, 3.0f, 11.286f, 3.606f, 9.821f, 4.638f)
                curveTo(9.964f, 4.467f, 10.05f, 4.247f, 10.051f, 4.006f)
                curveTo(10.055f, 3.454f, 9.61f, 3.004f, 9.058f, 3.0f)
                lineTo(9.0f, 3.0f)
                close()
                moveTo(12.132f, 5.612f)
                curveTo(9.696f, 6.708f, 8.0f, 9.156f, 8.0f, 12.0f)
                curveTo(8.0f, 14.844f, 9.696f, 17.292f, 12.132f, 18.388f)
                curveTo(12.215f, 18.267f, 12.326f, 18.163f, 12.461f, 18.086f)
                curveTo(12.487f, 18.071f, 12.513f, 18.056f, 12.538f, 18.041f)
                curveTo(13.015f, 17.762f, 13.627f, 17.921f, 13.907f, 18.397f)
                curveTo(14.006f, 18.566f, 14.05f, 18.753f, 14.044f, 18.935f)
                curveTo(14.357f, 18.978f, 14.676f, 19.0f, 15.0f, 19.0f)
                curveTo(18.866f, 19.0f, 22.0f, 15.866f, 22.0f, 12.0f)
                curveTo(22.0f, 8.134f, 18.866f, 5.0f, 15.0f, 5.0f)
                curveTo(14.676f, 5.0f, 14.357f, 5.022f, 14.044f, 5.065f)
                curveTo(14.05f, 5.247f, 14.006f, 5.434f, 13.907f, 5.603f)
                curveTo(13.627f, 6.079f, 13.015f, 6.238f, 12.538f, 5.959f)
                curveTo(12.513f, 5.944f, 12.487f, 5.929f, 12.461f, 5.914f)
                curveTo(12.326f, 5.837f, 12.215f, 5.733f, 12.132f, 5.612f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF46464F)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(5.539f, 5.914f)
                curveTo(6.019f, 5.64f, 6.186f, 5.03f, 5.912f, 4.55f)
                curveTo(5.639f, 4.07f, 5.028f, 3.903f, 4.548f, 4.176f)
                curveTo(4.515f, 4.195f, 4.482f, 4.215f, 4.449f, 4.234f)
                curveTo(3.973f, 4.514f, 3.813f, 5.126f, 4.093f, 5.603f)
                curveTo(4.373f, 6.079f, 4.985f, 6.238f, 5.462f, 5.959f)
                curveTo(5.487f, 5.944f, 5.513f, 5.929f, 5.539f, 5.914f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF46464F)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(2.959f, 8.462f)
                curveTo(3.238f, 7.985f, 3.079f, 7.373f, 2.603f, 7.093f)
                curveTo(2.126f, 6.813f, 1.514f, 6.973f, 1.234f, 7.449f)
                curveTo(1.215f, 7.482f, 1.195f, 7.515f, 1.176f, 7.548f)
                curveTo(0.903f, 8.028f, 1.07f, 8.639f, 1.55f, 8.912f)
                curveTo(2.03f, 9.186f, 2.64f, 9.019f, 2.914f, 8.539f)
                curveTo(2.929f, 8.513f, 2.944f, 8.487f, 2.959f, 8.462f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF46464F)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(2.0f, 11.955f)
                curveTo(2.004f, 11.403f, 1.559f, 10.952f, 1.006f, 10.949f)
                curveTo(0.454f, 10.945f, 0.004f, 11.39f, 0.0f, 11.942f)
                lineTo(0.0f, 12.0f)
                lineTo(0.0f, 12.058f)
                curveTo(0.004f, 12.61f, 0.454f, 13.055f, 1.006f, 13.051f)
                curveTo(1.559f, 13.048f, 2.004f, 12.597f, 2.0f, 12.045f)
                lineTo(2.0f, 12.0f)
                lineTo(2.0f, 11.955f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF46464F)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(2.914f, 15.461f)
                curveTo(2.64f, 14.981f, 2.03f, 14.814f, 1.55f, 15.088f)
                curveTo(1.07f, 15.361f, 0.903f, 15.972f, 1.176f, 16.452f)
                curveTo(1.195f, 16.485f, 1.215f, 16.518f, 1.234f, 16.551f)
                curveTo(1.514f, 17.027f, 2.126f, 17.187f, 2.603f, 16.907f)
                curveTo(3.079f, 16.628f, 3.238f, 16.015f, 2.959f, 15.538f)
                curveTo(2.944f, 15.513f, 2.929f, 15.487f, 2.914f, 15.461f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF46464F)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(5.462f, 18.041f)
                curveTo(4.985f, 17.762f, 4.373f, 17.921f, 4.093f, 18.397f)
                curveTo(3.813f, 18.874f, 3.973f, 19.486f, 4.449f, 19.766f)
                curveTo(4.482f, 19.785f, 4.515f, 19.805f, 4.548f, 19.824f)
                curveTo(5.028f, 20.097f, 5.639f, 19.93f, 5.912f, 19.45f)
                curveTo(6.186f, 18.97f, 6.019f, 18.36f, 5.539f, 18.086f)
                curveTo(5.513f, 18.071f, 5.487f, 18.056f, 5.462f, 18.041f)
                close()
            }
        }
            .build()
        return animation!!
    }

private var animation: ImageVector? = null

@Preview
@Composable
private fun Preview() {
    Box(modifier = Modifier.padding(12.dp)) {
        Image(imageVector = IconPack.Animation, contentDescription = "")
    }
}
