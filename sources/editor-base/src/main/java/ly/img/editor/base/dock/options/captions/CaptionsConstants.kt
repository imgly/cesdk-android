package ly.img.editor.base.dock.options.captions

import androidx.compose.ui.unit.dp
import ly.img.editor.base.timeline.state.TimelineConfiguration
import ly.img.editor.core.library.data.AssetSourceType
import kotlin.time.DurationUnit

/** Duration of a manually created caption, in seconds. */
internal const val DEFAULT_CAPTION_DURATION_SECONDS = 3.0

/**
 * Shortest a caption inserted into a gap may be, in seconds.
 *
 * Taken from the timeline's own floor rather than restated: anything shorter is a clip the trim handles will
 * not let the user grab back.
 */
internal val MINIMUM_CAPTION_DURATION_SECONDS =
    TimelineConfiguration.minCaptionClipDuration.toDouble(DurationUnit.SECONDS)

/** The bundled caption style presets asset source. */
internal val CAPTION_PRESETS_SOURCE_ID = AssetSourceType.CaptionPresets.sourceId

/**
 * How far a preset's box may miss the size the caption already had before that size is scaled back.
 *
 * A preset reproducing the caption's own size leaves it untouched rather than rewriting its box and position
 * as a side effect.
 */
internal const val SIZE_RESTORE_EPSILON = 0.001F

/** The style preset applied to a caption created before any other preset has been chosen. */
internal const val DEFAULT_CAPTION_PRESET_ID = "ly.img.caption.presets.outline"

/** The property holding a caption's text content. Captions live in the `caption/` namespace, never `text/`. */
internal const val CAPTION_TEXT_PROPERTY = "caption/text"

/** Last applied preset's asset id, kept on the track rather than a caption so it is part of scene and undo state. */
internal const val CAPTION_APPLIED_PRESET_METADATA_KEY = "ly.img.caption.appliedPresetID"

/** How many lines of a caption a row shows before it scrolls. */
internal const val CAPTION_MAX_VISIBLE_LINES = 3

/** Corner radius of a caption row card and of the actions revealed behind it. */
internal val captionRowCornerRadius = 16.dp

/** Minimum caption row height, matching `CheckedTextRow`; otherwise the 48dp overflow touch target makes one-line rows oversized. */
internal val captionRowMinHeight = 56.dp

/** Width of the accent ring drawn around the selected caption row. */
internal val captionRowSelectionRingWidth = 2.dp

/** Width of one action revealed behind a caption row when it is swiped aside. */
internal val captionRowActionWidth = 64.dp

/** Swipe velocity per second that opens or closes a row regardless of distance, matching Compose's swipeable primitives. */
internal val captionRowFlingVelocity = 125.dp

/** Size of an icon button in the keyboard action bar. */
internal val captionActionBarButtonSize = 40.dp

/** How far a disabled action-bar button fades, matching Material's disabled content alpha. */
internal const val captionActionBarDisabledAlpha = 0.38F

/** Diameter of the progress indicator that replaces a sheet action button's label while it works. */
internal val captionActionButtonProgressSize = 18.dp

/** Stroke of that progress indicator, scaled down with it so it doesn't read as a solid disc. */
internal val captionActionButtonProgressStroke = 2.dp
