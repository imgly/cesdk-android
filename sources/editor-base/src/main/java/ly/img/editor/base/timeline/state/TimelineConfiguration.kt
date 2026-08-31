package ly.img.editor.base.timeline.state

import androidx.compose.ui.unit.dp
import ly.img.editor.base.timeline.clip.ClipType
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object TimelineConfiguration {
    var addClipOptions: List<AddClipOption> = listOf(
        AddClipOption.Camera,
        AddClipOption.Library,
    )

    var addAudioOptions: List<AddAudioOption> = listOf(
        AddAudioOption.Library,
        AddAudioOption.Voiceover,
    )

    val minClipDuration = 1.seconds

    /**
     * A spoken line routinely lasts well under a second, so captions trim against a much lower
     * floor than other clips.
     */
    val minCaptionClipDuration = 100.milliseconds

    /** The shortest duration a clip of [clipType] may be trimmed to. */
    fun minDuration(clipType: ClipType): Duration = if (clipType == ClipType.Caption) {
        minCaptionClipDuration
    } else {
        minClipDuration
    }

    val clipHeight = 40.dp

    val backgroundTrackDividerHeight = 1.dp

    val clipPadding = 4.dp

    /** Space reserved between adjacent clip backgrounds. */
    val clipEndGap = 1.dp

    val transitionSeamSize = 24.dp

    val compactTransitionSeamSize = 10.dp

    val largeTransitionSeamMinFreeClipWidth = 30.dp

    val transitionSeamIconSpacing = 2.dp

    val rulerHeight = 16.dp

    val headerHeight = 48.dp

    val textContentHorizontalPadding = 4.dp

    val audioWaveformBarWidth = 1.dp
    val audioWaveformBarGap = 1.dp
}
