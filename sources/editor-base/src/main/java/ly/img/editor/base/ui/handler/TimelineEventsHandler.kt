package ly.img.editor.base.ui.handler

import ly.img.editor.base.engine.containsAudio
import ly.img.editor.base.timeline.clip.Clip
import ly.img.editor.base.timeline.clip.ClipType
import ly.img.editor.base.timeline.dragdrop.DropTarget
import ly.img.editor.base.timeline.dragdrop.resolveAnchorPageIndex
import ly.img.editor.base.timeline.state.LiveTrimState
import ly.img.editor.base.timeline.state.TimelineConfiguration
import ly.img.editor.base.timeline.state.TimelineState
import ly.img.editor.base.timeline.state.computeLiveTrimOverrides
import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.core.R
import ly.img.editor.core.ui.EventsHandler
import ly.img.editor.core.ui.engine.BlockKind
import ly.img.editor.core.ui.engine.Scope
import ly.img.editor.core.ui.engine.getKindEnum
import ly.img.editor.core.ui.engine.getSafeBackgroundTrack
import ly.img.editor.core.ui.engine.isBackgroundTrack
import ly.img.editor.core.ui.inject
import ly.img.editor.core.ui.register
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine
import ly.img.engine.SplitOptions
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/** Registers handlers for timeline events: selection, trim, split, reorder, drag-drop. */
@Suppress("NAME_SHADOWING")
fun EventsHandler.timelineEvents(
    engine: () -> Engine,
    timelineState: () -> TimelineState,
    showError: (Int) -> Unit,
) {
    val engine by inject(engine)
    val timelineState by inject(timelineState)

    register<BlockEvent.OnToggleBackgroundTrackAttach> {
        timelineState.selectedClip?.let { clip ->
            val id = clip.id
            if (clip.isInBackgroundTrack) {
                engine.block.appendChild(parent = checkNotNull(engine.scene.getCurrentPage()), child = id)
            } else {
                val insertedBlockTimeOffset = engine.block.getTimeOffset(id)
                val backgroundTrack = engine.getSafeBackgroundTrack()
                val backgroundTrackChildren = engine.block.getChildren(backgroundTrack)

                // Find the slot in the background track closest to the current time offset.
                var insertionIndex = backgroundTrackChildren.size
                for ((index, child) in backgroundTrackChildren.withIndex()) {
                    val timeOffset = engine.block.getTimeOffset(child)
                    val duration = engine.block.getDuration(child)
                    if (insertedBlockTimeOffset < timeOffset + duration / 2) {
                        insertionIndex = index
                        break
                    }
                }

                engine.block.insertChild(parent = backgroundTrack, child = id, index = insertionIndex)
                if (engine.block.isScopeEnabled(id, Scope.LayerCrop) && engine.block.getKindEnum(id) != BlockKind.Sticker) {
                    engine.block.resetCrop(id)
                    engine.block.fillParent(id)
                }
            }
            engine.editor.addUndoStep()
        }
    }

    register<BlockEvent.OnDeselect> {
        timelineState.selectedClip?.let { clip ->
            engine.block.setSelected(clip.id, false)
        }
    }

    register<BlockEvent.OnToggleSelectBlock> {
        // deselect previously selected block before selecting new one
        val previouslySelectedBlock = timelineState.selectedClip?.id
        if (previouslySelectedBlock != null && it.block != previouslySelectedBlock) {
            engine.block.setSelected(previouslySelectedBlock, false)
        }

        engine.block.setSelected(it.block, !engine.block.isSelected(it.block))
    }

    fun setDuration(
        clip: Clip,
        duration: Duration,
    ) {
        val durationInSeconds = duration.toDouble(DurationUnit.SECONDS)
        engine.block.setDuration(clip.id, durationInSeconds)
        if (clip.clipType == ClipType.Audio || clip.clipType == ClipType.Video) {
            engine.block.setTrimLength(clip.trimmableId, durationInSeconds)
        }
    }

    fun setTimeOffset(
        designBlock: DesignBlock,
        timeOffset: Duration,
    ) {
        engine.block.setTimeOffset(designBlock, timeOffset.toDouble(DurationUnit.SECONDS))
    }

    fun setTrimOffset(
        designBlock: DesignBlock,
        trimOffset: Duration,
    ) {
        engine.block.setTrimOffset(designBlock, trimOffset.toDouble(DurationUnit.SECONDS))
    }

    /**
     * After a trim commit, push unlocked siblings outward to clear residual overlaps so the
     * persisted layout matches the live preview. No-op for single-clip tracks.
     */
    fun packAndPersistSiblings(selectedClip: Clip) {
        val track = timelineState.dataSource.findTrack(selectedClip)
        if (track.clips.size < 2) return

        val newOffset = engine.block.getTimeOffset(selectedClip.id).seconds
        val newDuration = engine.block.getDuration(selectedClip.id).seconds

        // Sort by pre-commit offsets so the dragged clip keeps its live-preview index;
        // the new offset could push it past unshifted siblings. Substitute its new
        // offset/duration if the async refresh hasn't applied them yet.
        val sorted = track.clips
            .mapTo(ArrayList(track.clips.size)) { sibling ->
                if (sibling.id == selectedClip.id &&
                    (sibling.timeOffset != selectedClip.timeOffset || sibling.duration != selectedClip.duration)
                ) {
                    sibling.copy(timeOffset = selectedClip.timeOffset, duration = selectedClip.duration)
                } else {
                    sibling
                }
            }
        sorted.sortBy { it.timeOffset }
        val overrides = computeLiveTrimOverrides(
            sorted = sorted,
            trim = LiveTrimState(
                clipId = selectedClip.id,
                start = newOffset,
                end = newOffset + newDuration,
            ),
        )
        overrides.forEach { (id, offset) ->
            engine.block.setTimeOffset(id, offset.toDouble(DurationUnit.SECONDS))
        }
    }

    register<BlockEvent.OnUpdateTrim> {
        val selectedClip = checkNotNull(timelineState.selectedClip)
        val selectedBlock = selectedClip.id
        setTimeOffset(selectedBlock, it.timeOffset)
        // `OnUpdateTrim` is called in general for updating timeOffset, trimOffset, and duration simultaneously
        // Don't update trimOffset if the clip doesn't support trimming
        if (selectedClip.allowsTrimming) {
            setTrimOffset(selectedClip.trimmableId, it.trimOffset)
        }
        setDuration(selectedClip, it.duration)
        packAndPersistSiblings(selectedClip)
        engine.editor.addUndoStep()
    }

    register<BlockEvent.OnUpdateDuration> {
        val selectedClip = checkNotNull(timelineState.selectedClip)
        setDuration(selectedClip, it.duration)
        packAndPersistSiblings(selectedClip)
        engine.editor.addUndoStep()
    }

    register<BlockEvent.OnSplit> {
        val playheadPosition = timelineState.playerState.playheadPosition
        val selectedClip = checkNotNull(timelineState.selectedClip)
        val originalClipDuration = selectedClip.duration
        val absoluteStartTime = selectedClip.timeOffset
        val minClipDuration = TimelineConfiguration.minClipDuration
        val absoluteEndTime = absoluteStartTime + originalClipDuration

        if (playheadPosition < absoluteStartTime || playheadPosition > absoluteEndTime) {
            showError(R.string.ly_img_editor_timeline_error_split_out_of_range)
            return@register
        }

        if (playheadPosition < absoluteStartTime + minClipDuration ||
            (playheadPosition > absoluteEndTime - minClipDuration)
        ) {
            showError(R.string.ly_img_editor_timeline_error_split_short_duration)
            return@register
        }

        val splitTime = (playheadPosition - absoluteStartTime).toDouble(DurationUnit.SECONDS)
        engine.block.split(
            block = selectedClip.id,
            atTime = splitTime,
            options = SplitOptions(createParentTrackIfNeeded = true),
        )

        engine.editor.addUndoStep()
    }

    register<BlockEvent.OnReorder> {
        // optimistically reorder manually
        val backgroundClips = timelineState.dataSource.backgroundTrack.clips
        val oldIndex = backgroundClips.indexOfFirst { clip ->
            clip.id == it.block
        }
        backgroundClips.add(it.newIndex, backgroundClips.removeAt(oldIndex))

        engine.block.insertChild(
            parent = engine.getSafeBackgroundTrack(),
            child = it.block,
            index = it.newIndex,
        )
        engine.editor.addUndoStep()
    }

    register<BlockEvent.OnApplyDrop> { event ->
        if (event.target is DropTarget.ExistingTrack) {
            val backgroundTrack = timelineState.dataSource.backgroundTrack
            if (event.target.trackId == backgroundTrack.id) {
                // Bg-as-target (any source): engine auto-packs offsets on insert, so the
                // commit is `insertChild` only — `setTimeOffset` would fight the auto-pack.
                // FG → BG also runs the sync rebuild + suppress so the clip doesn't flash
                // in its source row while the async refresh catches up; BG → BG stays in
                // its row, no flicker.
                val sourceTrack = timelineState.dataSource.findTrack(event.clip)
                engine.block.insertChild(
                    parent = engine.getSafeBackgroundTrack(),
                    child = event.clip.id,
                    index = event.target.insertIndex,
                )
                // FG → BG parity with OnToggleBackgroundTrackAttach: non-sticker blocks
                // entering the bg track reset their crop and fill the page so the bg
                // semantics (canvas-filling visual) hold regardless of how the clip got there.
                if (sourceTrack !== backgroundTrack &&
                    engine.block.isScopeEnabled(event.clip.id, Scope.LayerCrop) &&
                    engine.block.getKindEnum(event.clip.id) != BlockKind.Sticker
                ) {
                    engine.block.resetCrop(event.clip.id)
                    engine.block.fillParent(event.clip.id)
                }
                engine.editor.addUndoStep()
                if (sourceTrack !== backgroundTrack) {
                    timelineState.forceRefresh()
                    timelineState.dragDrop.overrides.clear()
                    timelineState.dragDrop.markSuppressNextEngineEventRefresh()
                }
                return@register
            }
        }

        // Trim-to-fit before the insertChild below so the engine doesn't see an
        // oversized clip overlap a locked sibling and bump it.
        val target = event.target
        if (target is DropTarget.ExistingTrack && target.effectiveDuration != null) {
            setDuration(event.clip, target.effectiveDuration)
        }

        // Critical ordering: every insertChild MUST run before any setTimeOffset writes
        // in the same commit. The engine's Track::layout() walks children in array
        // order and corrupts positions when setTimeOffsets land on out-of-order
        // children.
        //
        // When the dragged clip stays in its source row, skip the sync rebuild — the
        // async refresh's ~1-frame lag isn't visible and rebuilding tears down all
        // foreground TrackViews on every reorder. Preview overrides clear at the next async refresh.
        // When the clip moves between tracks, rebuild dataSource synchronously to avoid a flicker
        // through the source row, then suppress the matching async refresh and clear overrides inline.
        val needsSyncRebuild: Boolean = when (target) {
            is DropTarget.ExistingTrack -> {
                val sourceTrack = timelineState.dataSource.findTrack(event.clip)
                val targetTrack = timelineState.dataSource.tracks
                    .firstOrNull { it.id == target.trackId } ?: return@register
                val isCrossTrack = sourceTrack.id != targetTrack.id
                val targetEngineTrackId = targetTrack.engineTrackId

                when {
                    targetEngineTrackId != null -> {
                        // Within-track reorder or cross-track into a multi-clip target:
                        engine.block.insertChild(targetEngineTrackId, event.clip.id, target.insertIndex)
                        event.siblingOffsets.forEach { (id, offset) ->
                            engine.block.setTimeOffset(id, offset.toDouble(DurationUnit.SECONDS))
                        }
                        engine.block.setTimeOffset(event.clip.id, target.timeOffset.toDouble(DurationUnit.SECONDS))
                    }
                    isCrossTrack -> {
                        // Cross-track into a standalone target:
                        val pageId = engine.scene.getCurrentPage() ?: return@register
                        val pageChildren = engine.block.getChildren(pageId)
                        val targetSolo = targetTrack.clips.firstOrNull() ?: return@register
                        val pageIndex = pageChildren.indexOf(targetSolo.id)
                        if (pageIndex < 0) return@register

                        val newTrack = engine.block.create(DesignBlockType.Track)
                        engine.block.setBoolean(newTrack, TRACK_AUTO_OFFSET_KEY, false)
                        engine.block.insertChild(pageId, newTrack, pageIndex)
                        engine.block.appendChild(newTrack, targetSolo.id)
                        engine.block.insertChild(newTrack, event.clip.id, target.insertIndex)

                        val targetSoloOffset = event.siblingOffsets[targetSolo.id] ?: targetSolo.timeOffset
                        engine.block.setTimeOffset(targetSolo.id, targetSoloOffset.toDouble(DurationUnit.SECONDS))
                        engine.block.setTimeOffset(event.clip.id, target.timeOffset.toDouble(DurationUnit.SECONDS))
                    }
                    else -> {
                        // Standalone source, same standalone target:
                        engine.block.setTimeOffset(event.clip.id, target.timeOffset.toDouble(DurationUnit.SECONDS))
                    }
                }
                isCrossTrack
            }
            is DropTarget.NewTrack -> {
                // New track in the gap: create a fresh engine track at the right page
                // index so that after TimelineState.refresh rebuilds, the new track
                // lands at tracks[target.insertAt]. The anchor walk over pageChildren
                // accounts for the prepend/append split in TimelineDataSource.
                val pageId = engine.scene.getCurrentPage() ?: return@register
                val pageChildren = engine.block.getChildren(pageId)
                val anchorPageIndex = resolveAnchorPageIndex(
                    dragged = event.clip,
                    tracks = timelineState.dataSource.tracks,
                    pageChildren = pageChildren,
                    insertAt = target.insertAt,
                    isAudioBlock = { child -> engine.block.containsAudio(child) },
                    isBackgroundTrack = { child -> engine.block.isBackgroundTrack(child) },
                )
                val newTrack = engine.block.create(DesignBlockType.Track)
                engine.block.setBoolean(newTrack, TRACK_AUTO_OFFSET_KEY, false)
                engine.block.insertChild(pageId, newTrack, anchorPageIndex)
                engine.block.insertChild(newTrack, event.clip.id, 0)
                engine.block.setTimeOffset(event.clip.id, target.timeOffset.toDouble(DurationUnit.SECONDS))
                true
            }
        }

        engine.editor.addUndoStep()
        if (needsSyncRebuild) {
            timelineState.forceRefresh()
            timelineState.dragDrop.overrides.clear()
            timelineState.dragDrop.markSuppressNextEngineEventRefresh()
        }
    }
}

private const val TRACK_AUTO_OFFSET_KEY = "track/automaticallyManageBlockOffsets"
