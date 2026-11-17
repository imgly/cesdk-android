package ly.img.editor.base.dock.options.voiceover

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ly.img.editor.core.R
import ly.img.editor.core.theme.LocalExtendedColorScheme
import ly.img.editor.core.ui.iconpack.Mic
import ly.img.editor.core.ui.iconpack.Voiceoverstop
import java.util.Locale
import ly.img.editor.core.ui.iconpack.IconPack as CoreUiIconPack

@Composable
internal fun VoiceOverSheetBarButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    iconSize: Dp = 24.dp,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
        ),
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(voiceOverActionTileHeight)
                    .offset(y = voiceOverActionTileOffset),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    space = voiceOverRecordActionContentSpacing,
                    alignment = Alignment.Top,
                ),
            ) {
                Box(
                    modifier = Modifier
                        .width(voiceOverActionIconFrameWidth)
                        .height(voiceOverRecordIconFrameHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(voiceOverActionLabelHeight),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    VoiceOverSheetActionLabel(
                        text = label,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
internal fun VoiceOverSheetActionLabel(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines: Int = 1,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        ),
        color = color,
        textAlign = TextAlign.Center,
        softWrap = false,
        maxLines = maxLines,
        overflow = TextOverflow.Clip,
    )
}

@Composable
internal fun VoiceOverRecordActionButton(
    modifier: Modifier = Modifier,
    isRecording: Boolean,
    isSaving: Boolean,
    recordedDurationMs: Long,
    onClick: () -> Unit,
) {
    val pink = LocalExtendedColorScheme.current.pink
    val animatedOuterWidth by animateDpAsState(
        targetValue = if (isRecording) {
            voiceOverRecordButtonRecordingWidth
        } else {
            voiceOverRecordButtonIdleWidth
        },
        animationSpec = tween(
            durationMillis = VOICE_OVER_RECORD_BUTTON_ANIMATION_DURATION_MILLIS,
            easing = FastOutSlowInEasing,
        ),
        label = "VoiceOverRecordButtonOuterWidth",
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isRecording) pink.color else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(durationMillis = VOICE_OVER_RECORD_BUTTON_ANIMATION_DURATION_MILLIS),
        label = "VoiceOverRecordButtonBorderColor",
    )
    val animatedInnerContainerColor by animateColorAsState(
        targetValue = if (isRecording) pink.colorContainer else pink.color,
        animationSpec = tween(durationMillis = VOICE_OVER_RECORD_BUTTON_ANIMATION_DURATION_MILLIS),
        label = "VoiceOverRecordButtonInnerContainerColor",
    )
    val animatedContentColor by animateColorAsState(
        targetValue = if (isRecording) pink.color else MaterialTheme.colorScheme.inverseOnSurface,
        animationSpec = tween(durationMillis = VOICE_OVER_RECORD_BUTTON_ANIMATION_DURATION_MILLIS),
        label = "VoiceOverRecordButtonContentColor",
    )
    val animatedInnerWidth by animateDpAsState(
        targetValue = if (isRecording) {
            voiceOverRecordButtonInnerRecordingWidth
        } else {
            voiceOverRecordButtonInnerIdleWidth
        },
        animationSpec = tween(
            durationMillis = VOICE_OVER_RECORD_BUTTON_ANIMATION_DURATION_MILLIS,
            easing = FastOutSlowInEasing,
        ),
        label = "VoiceOverRecordButtonInnerWidth",
    )
    val symbolSize = if (isRecording) 12.dp else 24.dp
    Surface(
        modifier = modifier.width(animatedOuterWidth),
        onClick = onClick,
        enabled = !isSaving,
        shape = MaterialTheme.shapes.extraLarge,
        color = Color.Transparent,
        border = BorderStroke(voiceOverRecordButtonBorderWidth, animatedBorderColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(voiceOverRecordButtonInset),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .width(animatedInnerWidth)
                    .height(voiceOverRecordButtonInnerHeight),
                shape = MaterialTheme.shapes.extraLarge,
                color = animatedInnerContainerColor,
                contentColor = animatedContentColor,
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(
                        space = voiceOverRecordActionContentSpacing,
                        alignment = Alignment.Top,
                    ),
                ) {
                    Box(
                        modifier = Modifier
                            .width(voiceOverActionIconFrameWidth)
                            .height(voiceOverRecordIconFrameHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isRecording) {
                            Icon(
                                imageVector = CoreUiIconPack.Voiceoverstop,
                                contentDescription = null,
                                modifier = Modifier.size(symbolSize),
                            )
                        } else {
                            Icon(
                                imageVector = CoreUiIconPack.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(symbolSize),
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(voiceOverActionLabelHeight),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        VoiceOverSheetActionLabel(
                            text = if (isRecording) {
                                recordedDurationMs.formatVoiceOverRecordDuration()
                            } else {
                                stringResource(R.string.ly_img_editor_sheet_voiceover_button_record)
                            },
                            color = animatedContentColor,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

internal fun Long.formatVoiceOverRecordDuration(): String {
    val safe = coerceAtLeast(0L)
    val totalSeconds = safe / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    val centiseconds = (safe % 1000L) / 10L
    return String.format(Locale.getDefault(), "%02d:%02d,%02d", minutes, seconds, centiseconds)
}
