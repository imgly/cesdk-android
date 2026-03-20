package ly.img.editor.base.dock.options.voiceover

import androidx.compose.ui.unit.dp

internal const val VOICEOVER_KIND = "voiceover"
internal const val SAMPLE_RATE = 48000
internal const val PCM_BYTES_PER_SAMPLE = 2
internal const val WAVE_FORMAT_IEEE_FLOAT = 3
internal const val VOICEOVER_WAV_CHANNELS = 2
internal const val VOICEOVER_WAV_BITS_PER_SAMPLE = 32
internal const val VOICEOVER_WAV_BYTES_PER_SAMPLE = VOICEOVER_WAV_BITS_PER_SAMPLE / 8
internal const val VOICEOVER_WAV_BLOCK_ALIGN = VOICEOVER_WAV_CHANNELS * VOICEOVER_WAV_BYTES_PER_SAMPLE
internal const val PLAYBACK_END_EPSILON_SECONDS = 0.02
internal const val PLAYBACK_PAUSE_STOP_GRACE_SECONDS = 0.35
internal const val PLAYBACK_EXTENSION_BUFFER_SECONDS = 0.5
internal const val ENGINE_WAVEFORM_UPDATE_INTERVAL_MS = 300L
internal const val ENGINE_BUFFER_CAPACITY_GROWTH_BYTES = SAMPLE_RATE * VOICEOVER_WAV_BLOCK_ALIGN * 2

internal const val VOICE_OVER_RECORD_BUTTON_ANIMATION_DURATION_MILLIS = 220
internal val voiceOverRecordSheetHeight = 100.dp
internal val voiceOverRecordSheetTopInset = 8.dp
internal val voiceOverRecordSheetFigmaWidth = 412.dp
internal val voiceOverRecordSheetHorizontalPadding = 32.dp
internal val voiceOverRecordSheetCompactHorizontalPadding = 16.dp
internal val voiceOverRecordSheetControlsHeight = 64.dp
internal val voiceOverSheetActionMinWidth = 72.dp
internal val voiceOverSheetActionWidth = 80.dp
internal val voiceOverSheetActionExpandedWidth = 120.dp
internal val voiceOverRecordButtonIdleWidth = 104.dp
internal val voiceOverRecordButtonRecordingWidth = 126.dp
internal val voiceOverRecordButtonInset = 4.dp
internal val voiceOverRecordButtonBorderWidth = 2.dp
internal val voiceOverRecordButtonInnerIdleWidth = 96.dp
internal val voiceOverRecordButtonInnerRecordingWidth = 118.dp
internal val voiceOverRecordButtonInnerHeight = 56.dp
internal val voiceOverActionTileOffset = 4.dp
internal val voiceOverActionTileHeight = 56.dp
internal val voiceOverActionIconFrameWidth = 40.dp
internal val voiceOverActionLabelHeight = 20.dp
internal val voiceOverRecordActionContentSpacing = 2.dp
internal val voiceOverRecordIconFrameHeight = 28.dp
