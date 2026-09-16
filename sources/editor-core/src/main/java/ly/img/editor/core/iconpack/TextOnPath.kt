package ly.img.editor.core.iconpack

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

val IconPack.TextOnPath: ImageVector
    get() {
        if (textOnPath != null) {
            return textOnPath!!
        }
        textOnPath = Builder(
            name = "TextOnPath",
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
                moveTo(11.0f, 12.0f)
                curveTo(14.708f, 12.0f, 17.943f, 14.02f, 19.669f, 17.018f)
                curveTo(19.778f, 17.007f, 19.888f, 17.0f, 20.0f, 17.0f)
                curveTo(21.657f, 17.0f, 23.0f, 18.343f, 23.0f, 20.0f)
                curveTo(23.0f, 21.657f, 21.657f, 23.0f, 20.0f, 23.0f)
                curveTo(18.343f, 23.0f, 17.0f, 21.657f, 17.0f, 20.0f)
                curveTo(17.0f, 19.18f, 17.33f, 18.436f, 17.863f, 17.895f)
                curveTo(16.465f, 15.563f, 13.917f, 14.0f, 11.0f, 14.0f)
                curveTo(9.46f, 14.0f, 8.024f, 14.436f, 6.804f, 15.19f)
                curveTo(6.928f, 15.52f, 7.0f, 15.877f, 7.0f, 16.25f)
                curveTo(7.0f, 17.907f, 5.657f, 19.25f, 4.0f, 19.25f)
                curveTo(2.343f, 19.25f, 1.0f, 17.907f, 1.0f, 16.25f)
                curveTo(1.0f, 14.593f, 2.343f, 13.25f, 4.0f, 13.25f)
                curveTo(4.546f, 13.25f, 5.056f, 13.398f, 5.497f, 13.652f)
                curveTo(7.076f, 12.609f, 8.966f, 12.0f, 11.0f, 12.0f)
                close()
                moveTo(20.0f, 19.0f)
                curveTo(19.448f, 19.0f, 19.0f, 19.448f, 19.0f, 20.0f)
                curveTo(19.0f, 20.552f, 19.448f, 21.0f, 20.0f, 21.0f)
                curveTo(20.552f, 21.0f, 21.0f, 20.552f, 21.0f, 20.0f)
                curveTo(21.0f, 19.448f, 20.552f, 19.0f, 20.0f, 19.0f)
                close()
                moveTo(4.0f, 15.25f)
                curveTo(3.448f, 15.25f, 3.0f, 15.698f, 3.0f, 16.25f)
                curveTo(3.0f, 16.802f, 3.448f, 17.25f, 4.0f, 17.25f)
                curveTo(4.552f, 17.25f, 5.0f, 16.802f, 5.0f, 16.25f)
                curveTo(5.0f, 15.698f, 4.552f, 15.25f, 4.0f, 15.25f)
                close()
                moveTo(21.773f, 7.532f)
                curveTo(21.989f, 7.513f, 22.208f, 7.595f, 22.428f, 7.778f)
                curveTo(22.647f, 7.962f, 22.767f, 8.161f, 22.787f, 8.377f)
                curveTo(22.807f, 8.593f, 22.722f, 8.814f, 22.534f, 9.039f)
                lineTo(22.138f, 9.516f)
                lineTo(22.767f, 10.04f)
                curveTo(22.937f, 10.181f, 23.028f, 10.332f, 23.043f, 10.491f)
                curveTo(23.067f, 10.65f, 23.01f, 10.811f, 22.873f, 10.974f)
                curveTo(22.736f, 11.138f, 22.586f, 11.226f, 22.422f, 11.236f)
                curveTo(22.262f, 11.251f, 22.097f, 11.188f, 21.929f, 11.047f)
                lineTo(21.299f, 10.521f)
                lineTo(20.293f, 11.727f)
                curveTo(20.135f, 11.917f, 20.061f, 12.094f, 20.071f, 12.259f)
                curveTo(20.091f, 12.422f, 20.198f, 12.586f, 20.392f, 12.748f)
                curveTo(20.464f, 12.808f, 20.54f, 12.857f, 20.619f, 12.897f)
                curveTo(20.698f, 12.938f, 20.767f, 12.981f, 20.823f, 13.028f)
                curveTo(20.909f, 13.091f, 20.955f, 13.172f, 20.959f, 13.271f)
                curveTo(20.967f, 13.365f, 20.892f, 13.507f, 20.734f, 13.696f)
                curveTo(20.606f, 13.85f, 20.475f, 13.961f, 20.339f, 14.03f)
                curveTo(20.207f, 14.094f, 20.063f, 14.101f, 19.907f, 14.049f)
                curveTo(19.814f, 14.015f, 19.7f, 13.953f, 19.564f, 13.866f)
                curveTo(19.424f, 13.784f, 19.316f, 13.711f, 19.239f, 13.647f)
                curveTo(18.855f, 13.327f, 18.586f, 13.004f, 18.434f, 12.677f)
                curveTo(18.286f, 12.354f, 18.25f, 12.023f, 18.323f, 11.686f)
                curveTo(18.401f, 11.342f, 18.589f, 10.992f, 18.888f, 10.634f)
                lineTo(19.932f, 9.382f)
                lineTo(19.67f, 9.164f)
                curveTo(19.506f, 9.027f, 19.415f, 8.877f, 19.395f, 8.713f)
                curveTo(19.381f, 8.553f, 19.441f, 8.391f, 19.578f, 8.227f)
                curveTo(19.715f, 8.064f, 19.861f, 7.977f, 20.017f, 7.968f)
                curveTo(20.181f, 7.958f, 20.345f, 8.021f, 20.509f, 8.157f)
                lineTo(20.77f, 8.375f)
                lineTo(21.167f, 7.899f)
                curveTo(21.355f, 7.674f, 21.557f, 7.552f, 21.773f, 7.532f)
                close()
                moveTo(14.878f, 5.79f)
                curveTo(15.042f, 5.719f, 15.229f, 5.719f, 15.437f, 5.789f)
                curveTo(15.64f, 5.857f, 15.792f, 5.947f, 15.895f, 6.06f)
                curveTo(16.007f, 6.167f, 16.097f, 6.317f, 16.166f, 6.509f)
                lineTo(16.497f, 7.465f)
                lineTo(17.341f, 6.905f)
                curveTo(17.512f, 6.794f, 17.674f, 6.729f, 17.828f, 6.711f)
                curveTo(17.984f, 6.686f, 18.16f, 6.706f, 18.356f, 6.772f)
                curveTo(18.571f, 6.845f, 18.719f, 6.958f, 18.801f, 7.112f)
                curveTo(18.889f, 7.269f, 18.903f, 7.435f, 18.844f, 7.612f)
                curveTo(18.79f, 7.791f, 18.657f, 7.947f, 18.444f, 8.079f)
                lineTo(17.059f, 8.945f)
                lineTo(17.715f, 10.65f)
                curveTo(17.807f, 10.878f, 17.82f, 11.08f, 17.754f, 11.255f)
                curveTo(17.688f, 11.429f, 17.571f, 11.555f, 17.404f, 11.633f)
                curveTo(17.24f, 11.704f, 17.05f, 11.703f, 16.835f, 11.631f)
                curveTo(16.639f, 11.565f, 16.485f, 11.478f, 16.374f, 11.37f)
                curveTo(16.265f, 11.256f, 16.176f, 11.103f, 16.107f, 10.911f)
                lineTo(15.709f, 9.783f)
                lineTo(14.705f, 10.438f)
                curveTo(14.542f, 10.545f, 14.383f, 10.611f, 14.226f, 10.636f)
                curveTo(14.077f, 10.663f, 13.9f, 10.642f, 13.698f, 10.574f)
                curveTo(13.49f, 10.504f, 13.342f, 10.391f, 13.254f, 10.234f)
                curveTo(13.166f, 10.078f, 13.149f, 9.91f, 13.202f, 9.731f)
                curveTo(13.262f, 9.555f, 13.394f, 9.399f, 13.601f, 9.265f)
                lineTo(15.145f, 8.301f)
                lineTo(14.568f, 6.772f)
                curveTo(14.487f, 6.534f, 14.475f, 6.33f, 14.532f, 6.159f)
                curveTo(14.598f, 5.984f, 14.713f, 5.861f, 14.878f, 5.79f)
                close()
                moveTo(5.678f, 3.594f)
                curveTo(5.898f, 3.483f, 6.093f, 3.456f, 6.263f, 3.512f)
                curveTo(6.439f, 3.565f, 6.582f, 3.702f, 6.693f, 3.922f)
                curveTo(6.801f, 4.136f, 6.826f, 4.334f, 6.767f, 4.513f)
                curveTo(6.71f, 4.683f, 6.572f, 4.823f, 6.352f, 4.934f)
                lineTo(4.949f, 5.64f)
                lineTo(7.065f, 9.847f)
                curveTo(7.197f, 10.109f, 7.232f, 10.349f, 7.17f, 10.566f)
                curveTo(7.105f, 10.778f, 6.936f, 10.953f, 6.662f, 11.091f)
                curveTo(6.4f, 11.223f, 6.161f, 11.253f, 5.946f, 11.183f)
                curveTo(5.728f, 11.106f, 5.554f, 10.936f, 5.422f, 10.674f)
                lineTo(3.305f, 6.466f)
                lineTo(1.902f, 7.172f)
                curveTo(1.682f, 7.283f, 1.484f, 7.311f, 1.308f, 7.258f)
                curveTo(1.135f, 7.195f, 0.994f, 7.057f, 0.887f, 6.843f)
                curveTo(0.776f, 6.623f, 0.749f, 6.428f, 0.805f, 6.258f)
                curveTo(0.867f, 6.085f, 1.008f, 5.943f, 1.228f, 5.832f)
                lineTo(5.678f, 3.594f)
                close()
                moveTo(10.191f, 5.244f)
                curveTo(10.557f, 5.217f, 10.894f, 5.253f, 11.202f, 5.351f)
                curveTo(11.509f, 5.441f, 11.778f, 5.592f, 12.008f, 5.803f)
                curveTo(12.236f, 6.006f, 12.415f, 6.26f, 12.545f, 6.564f)
                curveTo(12.681f, 6.869f, 12.763f, 7.214f, 12.792f, 7.6f)
                curveTo(12.802f, 7.732f, 12.772f, 7.834f, 12.704f, 7.906f)
                curveTo(12.635f, 7.971f, 12.522f, 8.011f, 12.362f, 8.022f)
                lineTo(9.523f, 8.231f)
                curveTo(9.588f, 8.488f, 9.694f, 8.681f, 9.843f, 8.809f)
                curveTo(10.07f, 8.993f, 10.404f, 9.069f, 10.843f, 9.037f)
                curveTo(10.989f, 9.026f, 11.154f, 8.997f, 11.338f, 8.95f)
                curveTo(11.528f, 8.896f, 11.706f, 8.829f, 11.874f, 8.75f)
                curveTo(12.042f, 8.671f, 12.191f, 8.649f, 12.321f, 8.686f)
                curveTo(12.45f, 8.717f, 12.552f, 8.783f, 12.626f, 8.885f)
                curveTo(12.706f, 8.979f, 12.755f, 9.093f, 12.771f, 9.226f)
                curveTo(12.787f, 9.351f, 12.767f, 9.48f, 12.71f, 9.611f)
                curveTo(12.652f, 9.736f, 12.549f, 9.836f, 12.401f, 9.914f)
                curveTo(12.177f, 10.038f, 11.933f, 10.136f, 11.671f, 10.209f)
                curveTo(11.416f, 10.281f, 11.155f, 10.327f, 10.89f, 10.347f)
                curveTo(10.291f, 10.391f, 9.765f, 10.326f, 9.31f, 10.152f)
                curveTo(8.863f, 9.971f, 8.508f, 9.697f, 8.247f, 9.329f)
                curveTo(7.986f, 8.961f, 7.836f, 8.513f, 7.797f, 7.988f)
                curveTo(7.76f, 7.483f, 7.837f, 7.033f, 8.028f, 6.638f)
                curveTo(8.226f, 6.236f, 8.509f, 5.914f, 8.879f, 5.673f)
                curveTo(9.255f, 5.424f, 9.693f, 5.281f, 10.191f, 5.244f)
                close()
                moveTo(10.355f, 6.386f)
                curveTo(10.149f, 6.401f, 9.974f, 6.464f, 9.828f, 6.575f)
                curveTo(9.689f, 6.679f, 9.586f, 6.827f, 9.52f, 7.018f)
                curveTo(9.482f, 7.129f, 9.46f, 7.255f, 9.451f, 7.394f)
                lineTo(11.298f, 7.258f)
                curveTo(11.274f, 7.071f, 11.231f, 6.918f, 11.168f, 6.797f)
                curveTo(11.09f, 6.649f, 10.982f, 6.54f, 10.844f, 6.47f)
                curveTo(10.712f, 6.399f, 10.548f, 6.371f, 10.355f, 6.386f)
                close()
            }
        }
            .build()
        return textOnPath!!
    }

private var textOnPath: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.TextOnPath.IconPreview()
