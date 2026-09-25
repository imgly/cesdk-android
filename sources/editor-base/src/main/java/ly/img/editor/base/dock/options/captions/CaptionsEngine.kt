package ly.img.editor.base.dock.options.captions

import ly.img.editor.core.ui.engine.BlockKind
import ly.img.editor.core.ui.engine.getCurrentPage
import ly.img.engine.Asset
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine
import ly.img.engine.HorizontalBlockAlignment
import ly.img.engine.SizeMode
import ly.img.engine.VerticalBlockAlignment
import kotlin.math.abs

/**
 * The single place that talks to the engine about captions.
 *
 * - Never loop a style setter: a preset is applied to ONE caption and the engine syncs the style to its
 *   siblings.
 * - Add a caption by *copying* its neighbour, never by creating and styling a blank one — attaching a block
 *   replays its synced properties one at a time, which drifts a rotated caption. See [detachedCopy].
 * - Attach before styling — the engine's style sync only reaches scene-attached blocks.
 * - Guard every read with `isValid`: a merge or a delete can destroy a block a row still references.
 * - Caption properties live under the `caption/` namespace, never `text/`.
 *
 * Every mutation ends in a single `addUndoStep`. All engine calls happen on the main thread.
 */
internal class CaptionsEngine(
    private val engine: Engine,
    private val onError: (Throwable) -> Unit = {},
) {
    // region Reads

    /** The captions on the current page's caption track, in track order; empty when there is no track yet. */
    fun captions(): List<DesignBlock> {
        val track = captionTrack() ?: return emptyList()
        return captionChildren(track)
    }

    /** The text of a caption, or an empty string if the block is no longer valid. */
    fun text(caption: DesignBlock): String {
        if (!isValid(caption)) return ""
        return runCatching { engine.block.getString(caption, CAPTION_TEXT_PROPERTY) }.getOrDefault("")
    }

    /** The caption currently selected on the canvas, so the sheet can open on that row. */
    fun selectedCaption(): DesignBlock? = runCatching {
        engine.block.findAllSelected().firstOrNull { isValid(it) && isCaption(it) }
    }.getOrNull()

    /**
     * Whether the current page holds anything transcribable, so Generate can be offered only when it could do
     * something. Audio is matched by type because a voiceover's kind is `voiceover`, not `audio` — the same rule
     * the auto-captions plugin uses to pick its sources.
     *
     * Scoped to the page for the same reason the plugin scopes its candidates: otherwise the action offers to
     * transcribe a page the user is not on, and generation then finds nothing.
     */
    fun hasAudioVisualContent(): Boolean = runCatching {
        val page = currentPage() ?: return false
        val candidates = engine.block.findByType(DesignBlockType.Audio) + engine.block.findByKind(BlockKind.Video.key)
        candidates.any { isDescendant(it, page) }
    }.getOrDefault(false)

    /** Whether a block sits anywhere below [page] — directly, or nested in one of its tracks. */
    private fun isDescendant(
        block: DesignBlock,
        page: DesignBlock,
    ): Boolean {
        var current = block
        while (true) {
            val parent = runCatching { engine.block.getParent(current) }.getOrNull() ?: return false
            if (parent == page) return true
            current = parent
        }
    }

    /** The single caption track on the current page, if one exists. */
    fun captionTrack(): DesignBlock? {
        val page = currentPage() ?: return null
        val children = runCatching { engine.block.getChildren(page) }.getOrDefault(emptyList())
        return children.firstOrNull { isValid(it) && typeOf(it) == DesignBlockType.CaptionTrack.key }
    }

    // endregion
    // region Selection and playback

    /**
     * Selects a caption and previews it: playback pauses and the playhead moves to its start, clamped into
     * the page. Skipped when it is already selected, so this is safe to call on every focus change.
     */
    fun revealCaption(caption: DesignBlock) {
        val page = currentPage() ?: return
        if (!isValid(caption)) return
        if (runCatching { engine.block.isSelected(caption) }.getOrDefault(false)) return
        runCatching { engine.block.select(caption) }.onFailure(onError)
        pausePlayback(page)
        val offset = timeOffsetOf(caption)
        runCatching { engine.block.setPlaybackTime(page, clampToPage(offset, page)) }
    }

    /** Stops playback. Guarded: `setPlaying(page, false)` flips edit mode to TRANSFORM even when idle, tearing down text editing. */
    private fun pausePlayback(page: DesignBlock) {
        if (!runCatching { engine.block.isPlaying(page) }.getOrDefault(false)) return
        runCatching { engine.block.setPlaying(page, false) }
    }

    // endregion
    // region Mutations (each a single undo step)

    /**
     * Appends a caption to the track and moves the playhead to it: a *copy* of the last caption when there is
     * one, so it arrives sized, rotated, placed, styled and animated like the track it joins — or, on an empty
     * track, a fresh caption styled with the default preset and centred. One undo step covers the caption
     * *and* any track it had to create.
     *
     * @return the created caption, so the caller can put it straight into edit mode.
     */
    suspend fun createCaption(): DesignBlock? {
        val page = currentPage() ?: return null
        val hadTrack = captionTrack() != null
        var createdTrack: DesignBlock? = null
        var createdCaption: DesignBlock? = null
        return try {
            val track = findOrCreateCaptionTrack(page)
            // Remembered only when this operation created it, so a mid-way failure can roll it back.
            if (!hadTrack) createdTrack = track
            // Read before the new block is attached, so it can't resolve to the new caption itself.
            val last = captionChildren(track).lastOrNull()
            val caption: DesignBlock
            if (last != null) {
                // Copied rather than created: attaching replays a block's synced properties one at a time, and
                // rotation is replayed before width, so a rotated track compounds a placement error on every add.
                caption = detachedCopy(last, offset = timeOffsetOf(last) + durationOf(last))
                createdCaption = caption
                engine.block.appendChild(track, caption)
            } else {
                caption = engine.block.create(DesignBlockType.Caption)
                createdCaption = caption
                engine.block.appendChild(track, caption) // attach before styling
                engine.block.setDuration(caption, DEFAULT_CAPTION_DURATION_SECONDS)
                // No time offset: the track's first caption keeps the engine's default of 0 and the playhead
                // comes to it, rather than the caption landing wherever the playhead happens to sit.
                applyDefaultStyling(caption)
                centerCaption(caption)
            }

            runCatching { engine.block.setPlaybackTime(page, clampToPage(timeOffsetOf(caption), page)) }

            engine.editor.addUndoStep()
            caption
        } catch (throwable: Throwable) {
            rollBack(createdTrack, createdCaption)
            onError(throwable)
            null
        }
    }

    /** Sets the text of a caption. No-op if the block is no longer valid. */
    fun setText(
        text: String,
        caption: DesignBlock,
    ) {
        if (!isValid(caption)) return
        runCatching {
            engine.block.setString(caption, CAPTION_TEXT_PROPERTY, text)
            engine.editor.addUndoStep()
        }.onFailure(onError)
    }

    /** Deletes a caption, removing the caption track too if it becomes empty. One undo step. */
    fun deleteCaption(caption: DesignBlock) {
        if (!isValid(caption)) return
        runCatching {
            val track = captionTrack()
            engine.block.destroy(caption)
            if (track != null && isValid(track) && engine.block.getChildren(track).isEmpty()) {
                engine.block.destroy(track)
            }
            engine.editor.addUndoStep()
        }.onFailure(onError)
    }

    /**
     * Inserts a copy of the given caption immediately after it, sized to the gap that follows so the rest of
     * the track stays where it is.
     *
     * @return the created caption, so the caller can put it straight into edit mode.
     */
    suspend fun addCaptionAfter(caption: DesignBlock): DesignBlock? {
        if (!isValid(caption)) return null
        val track = captionTrack() ?: return null
        var createdCaption: DesignBlock? = null
        return try {
            val siblings = captionChildren(track)
            val index = siblings.indexOf(caption)
            if (index < 0) return null
            val end = timeOffsetOf(caption) + durationOf(caption)
            val nextStart = siblings.getOrNull(index + 1)?.let { timeOffsetOf(it) }

            val new = detachedCopy(
                caption,
                offset = end,
                duration = CaptionTiming.insertedDuration(start = end, nextStart = nextStart),
            )
            createdCaption = new
            engine.block.insertChild(track, new, index + 1)
            engine.editor.addUndoStep()
            new
        } catch (throwable: Throwable) {
            rollBack(createdTrack = null, createdCaption = createdCaption)
            onError(throwable)
            null
        }
    }

    /**
     * Merges a caption with its previous sibling: the texts are space-joined and the merged caption spans
     * from the previous start to this one's end. A no-op for the first caption.
     *
     * @param keepingCurrent `true` destroys the previous block and keeps [caption] — use it while the caption
     * is being edited, so the focused field (and its keyboard, caret and action bar) survives. `false` keeps
     * the previous block, so the list collapses in place instead of shuffling a row upwards.
     * @return the surviving caption, or `null` if nothing merged.
     */
    fun mergeWithPrevious(
        caption: DesignBlock,
        keepingCurrent: Boolean,
    ): DesignBlock? {
        if (!isValid(caption)) return null
        val track = captionTrack() ?: return null
        return try {
            val siblings = captionChildren(track)
            val index = siblings.indexOf(caption)
            if (index <= 0) return null
            val previous = siblings[index - 1]

            val joined = CaptionTiming.mergedText(previousText = text(previous), currentText = text(caption))

            val previousStart = timeOffsetOf(previous)
            val mergedDuration = CaptionTiming.mergedDuration(
                previousStart = previousStart,
                previousDuration = durationOf(previous),
                currentStart = timeOffsetOf(caption),
                currentDuration = durationOf(caption),
            )

            // The survivor is written before the absorbed sibling is destroyed, so a throw part-way leaves both blocks intact.
            val survivor = if (keepingCurrent) caption else previous
            engine.block.setString(survivor, CAPTION_TEXT_PROPERTY, joined)
            engine.block.setTimeOffset(survivor, previousStart)
            engine.block.setDuration(survivor, mergedDuration)
            engine.block.destroy(if (keepingCurrent) previous else caption)
            engine.editor.addUndoStep()
            survivor
        } catch (throwable: Throwable) {
            onError(throwable)
            null
        }
    }

    /**
     * Splits a caption at a caret offset: the text after the caret moves to a new caption inserted
     * immediately after, and the duration is divided in proportion to the character split — a caret carries no
     * time of its own.
     *
     * @param caretUtf16 the split point, as the UTF-16 offset the field reports for the caret.
     * @return the new caption, or `null` — leaving the scene untouched — when the offset doesn't divide the
     * text in two (at either end, out of range, or mid-surrogate-pair), or the caption is too short to hold
     * two halves.
     */
    fun splitCaption(
        caption: DesignBlock,
        caretUtf16: Int,
    ): DesignBlock? {
        if (!isValid(caption)) return null
        val text = text(caption)
        val texts = CaptionTiming.splitTexts(text, caretUtf16) ?: return null
        val (leftDuration, _) = CaptionTiming.splitDurations(
            totalDuration = durationOf(caption),
            leftLength = texts.first.length,
            totalLength = text.length,
        )
        return splitCaption(caption, texts, leftDuration)
    }

    /**
     * Splits a caption where the playhead sits, dividing the text at the word gap nearest to it — the generic
     * clip split would leave both halves holding the whole line.
     *
     * The playhead, not the snapped character count, sets the duration, so the clip edge lands where the user
     * aimed rather than moving by however far the snap travelled.
     *
     * @return the new caption holding the tail, or `null` when the playhead is outside the caption or so close
     * to an edge that one side would be empty.
     */
    fun splitCaptionAtPlayhead(
        caption: DesignBlock,
        playheadSeconds: Double,
    ): DesignBlock? {
        if (!isValid(caption)) return null
        val start = timeOffsetOf(caption)
        val duration = durationOf(caption)
        if (duration <= 0.0 || playheadSeconds <= start || playheadSeconds >= start + duration) return null
        val text = text(caption)
        val proportional = CaptionTiming.splitOffsetAtTime(
            textLength = text.length,
            start = start,
            duration = duration,
            playhead = playheadSeconds,
        )
        val offset = CaptionTiming.wordBoundary(text, nearestTo = proportional) ?: return null
        val texts = CaptionTiming.splitTexts(text, offset) ?: return null
        return splitCaption(caption, texts, leftDuration = playheadSeconds - start)
    }

    /**
     * The half both splits share. The tail takes the remainder, so the pair tiles exactly the range the
     * original occupied with no float drift. The new caption is a duplicate, so it inherits the style and
     * animations without re-running the preset apply.
     */
    private fun splitCaption(
        caption: DesignBlock,
        texts: Pair<String, String>,
        leftDuration: Double,
    ): DesignBlock? {
        val track = captionTrack() ?: return null
        // Captured for the rollback path, which has to restore the original if it was already shortened.
        val originalText = text(caption)
        val originalDuration = durationOf(caption)
        var createdCaption: DesignBlock? = null
        return try {
            val index = captionChildren(track).indexOf(caption)
            if (index < 0) return null

            val (leftText, rightText) = texts
            val offset = timeOffsetOf(caption)
            val rightDuration = (originalDuration - leftDuration).coerceAtLeast(0.0)

            // Every split funnels through here, so the floor only has to hold once: the timeline button
            // greys itself out, but the caret has no such affordance.
            if (!CaptionTiming.splitClearsFloor(originalDuration, leftDuration)) return null

            val new = engine.block.duplicate(caption, attachToParent = false)
            createdCaption = new

            // Retimed while still detached: two captions on the same range would make the track ripple every following one forward.
            engine.block.setString(new, CAPTION_TEXT_PROPERTY, rightText)
            engine.block.setTimeOffset(new, offset + leftDuration)
            engine.block.setDuration(new, rightDuration)
            engine.block.setString(caption, CAPTION_TEXT_PROPERTY, leftText)
            engine.block.setDuration(caption, leftDuration)

            engine.block.insertChild(track, new, index + 1)

            engine.editor.addUndoStep()
            new
        } catch (throwable: Throwable) {
            if (createdCaption != null && isValid(createdCaption)) {
                runCatching { engine.block.destroy(createdCaption) }
            }
            // Restore the original in case the writes above already truncated it.
            if (isValid(caption)) {
                runCatching { engine.block.setString(caption, CAPTION_TEXT_PROPERTY, originalText) }
                runCatching { engine.block.setDuration(caption, originalDuration) }
            }
            onError(throwable)
            null
        }
    }

    /**
     * Replaces the caption track with the cues parsed from an SRT or VTT file. One undo step covers the whole
     * import.
     *
     * The cues come back detached, and they are appended to a *detached* track that is attached to the page
     * once: appending to an attached track re-runs the engine's style sync for every cue. Only the first
     * caption is styled — the engine fans that style out to its siblings. The previous track is destroyed
     * last, so a failure before that point leaves the existing captions untouched.
     *
     * Unlike the other mutations here, failures are rethrown rather than routed to [onError]: a file the user
     * picked can fail for reasons worth naming, and the sheet maps them to import-specific copy.
     *
     * @return the imported captions, in cue order.
     * @throws EmptyCaptionImportException when the file held no cues, leaving the scene untouched.
     */
    suspend fun importCaptions(uri: String): List<DesignBlock> {
        // An empty URI makes the engine return without ever invoking its callback, which suspends the caller forever.
        require(uri.isNotBlank()) { "Caption import URI must not be blank" }
        val page = checkNotNull(currentPage()) { "Captions can only be imported into a page" }
        val captions = engine.block.createCaptionsFromURI(uri)
        // Belt-and-braces, not a known engine behavior: a cueless file is expected to fail rather than return
        // an empty list (see EmptyCaptionImportException), but this is checked directly so a slip in that
        // invariant can't silently replace an existing track with nothing.
        if (captions.isEmpty()) throw EmptyCaptionImportException()

        val previousTrack = captionTrack()
        var createdTrack: DesignBlock? = null
        try {
            val track = engine.block.create(DesignBlockType.CaptionTrack)
            createdTrack = track
            captions.forEach { engine.block.appendChild(track, it) }
            engine.block.appendChild(page, track) // attach before styling

            val first = captions.first()
            // Applied to this ONE caption — the engine fans the style out to the siblings. Always the default
            // preset, never the one recorded on a track being replaced: an import brings in a whole new track, so
            // there is no style of its own to carry over. The stamp still lands on the new track, because
            // `stampAppliedPreset` records against the caption's parent rather than the page's first caption track.
            applyDefaultStyling(first)
            // After the preset, so its `replace` mode cannot reset the placement.
            centerCaption(first)
            // The playhead stays where it was: an import brings in a whole track, so there is no one caption to
            // move to. Creating a caption manually still seeks, because that caption is what the user is editing.

            // Destroyed last, so a failure above leaves the captions that were already there.
            if (previousTrack != null && isValid(previousTrack)) {
                engine.block.destroy(previousTrack)
            }

            engine.editor.addUndoStep()
            return captions
        } catch (throwable: Throwable) {
            // Roll the imported blocks back so a failed import leaves nothing behind. The captions are destroyed
            // individually as well: they are still detached if the track was never created.
            rollBack(createdTrack, createdCaption = null)
            captions.filter { isValid(it) }.forEach { runCatching { engine.block.destroy(it) } }
            throw throwable
        }
    }

    /**
     * A blank copy of a caption, detached and timed to start at [offset] for [duration] — ready to attach.
     *
     * Copying rather than creating and styling is what keeps the rest of the track still: attaching a block
     * seeds its synced properties one at a time, so a rotated caption has its rotation replayed while its width
     * is still the newcomer's default, and the position the engine computes to compensate is measured against a
     * half-built box. That error fans out to every sibling through the position sync. A copy already agrees with
     * its siblings on every synced property, so the seeding writes nothing — and it carries the style and the
     * animations across, which no property sync does.
     *
     * Retimed while still detached: attaching first would put two captions on one range, which the track ripples
     * apart.
     */
    private fun detachedCopy(
        caption: DesignBlock,
        offset: Double,
        duration: Double = DEFAULT_CAPTION_DURATION_SECONDS,
    ): DesignBlock {
        val copy = engine.block.duplicate(caption, attachToParent = false)
        return try {
            engine.block.setString(copy, CAPTION_TEXT_PROPERTY, "")
            engine.block.setTimeOffset(copy, offset)
            engine.block.setDuration(copy, duration)
            copy
        } catch (throwable: Throwable) {
            // Nothing else knows about the copy yet, so it leaks detached unless it is destroyed here.
            runCatching { engine.block.destroy(copy) }
            throw throwable
        }
    }

    /** Undoes the step an import just committed, for a cancel that raced it. */
    fun revertLastStep() {
        runCatching {
            if (engine.editor.canUndo()) engine.editor.undo()
        }.onFailure(onError)
    }

    /** Deletes every caption by destroying the caption track, returning the sheet to its Add state. */
    fun deleteAllCaptions() {
        val track = captionTrack() ?: return
        runCatching {
            engine.block.destroy(track)
            engine.editor.addUndoStep()
        }.onFailure(onError)
    }

    // endregion
    // region Styling

    /** Applies the default preset to one caption; a missing preset source leaves it unstyled rather than failing creation. */
    private suspend fun applyDefaultStyling(caption: DesignBlock) {
        runCatching {
            if (!engine.asset.findAllSources().contains(CAPTION_PRESETS_SOURCE_ID)) return@runCatching
            val preset = engine.asset.fetchAsset(CAPTION_PRESETS_SOURCE_ID, DEFAULT_CAPTION_PRESET_ID)
                ?: return@runCatching
            applyPreset(
                sourceId = CAPTION_PRESETS_SOURCE_ID,
                assetId = DEFAULT_CAPTION_PRESET_ID,
                asset = preset,
                caption = caption,
            )
        }
    }

    /**
     * Applies a style preset; the engine fans it out to every caption on the track. One undo step.
     *
     * Restyling changes the look only: a caption the user resized keeps that size, with the preset's typography
     * scaled to match. See [restoreCaptionBox].
     *
     * @return `true` when the preset was applied.
     */
    suspend fun applyStylePreset(
        sourceId: String,
        assetId: String,
        asset: Asset,
        caption: DesignBlock,
    ): Boolean {
        if (!isValid(caption)) return false
        val widthBefore = frameWidth(caption)
        val heightBefore = layoutHeight(caption)
        return runCatching {
            applyPreset(sourceId = sourceId, assetId = assetId, asset = asset, caption = caption)
            val ratio = sizeRestoreRatio(caption, widthBefore)
            if (ratio != null) {
                restoreCaptionBox(caption, ratio, heightBefore)
            } else if (widthBefore == null) {
                // Only an auto-sized caption gets centred: centring re-places the whole track, so a caption
                // that had a box the ratio simply failed to reproduce keeps the preset's placement instead.
                centerCaption(caption)
            }
            engine.editor.addUndoStep()
            true
        }.onFailure(onError).getOrDefault(false)
    }

    /**
     * Applies a preset to ONE caption and records it on the track; the engine fans the style out to the
     * siblings, so this must never be looped over them. Adds no undo step — the block-targeted apply
     * resolves to `applyStylePresetToBlock`, which leaves history alone.
     */
    private suspend fun applyPreset(
        sourceId: String,
        assetId: String,
        asset: Asset,
        caption: DesignBlock,
    ) {
        engine.asset.applyAssetSourceAsset(sourceId = sourceId, asset = asset, block = caption)
        stampAppliedPreset(caption, assetId = assetId)
    }

    /** The asset id of the last applied caption style preset, recorded on the caption track, or `null`. */
    fun appliedPresetIdentifier(): String? {
        val track = captionTrack() ?: return null
        if (!isValid(track)) return null
        return runCatching {
            if (!engine.block.hasMetadata(track, CAPTION_APPLIED_PRESET_METADATA_KEY)) return@runCatching null
            engine.block.getMetadata(track, CAPTION_APPLIED_PRESET_METADATA_KEY)?.let(::stampedAssetId)
        }.getOrNull()
    }

    /** Records the applied preset on the caption's *parent* track, so it targets the right one mid-import while a track is replaced. */
    private fun stampAppliedPreset(
        caption: DesignBlock,
        assetId: String,
    ) {
        val track = runCatching { engine.block.getParent(caption) }.getOrNull() ?: return
        if (!isValid(track)) return
        runCatching {
            engine.block.setMetadata(track, CAPTION_APPLIED_PRESET_METADATA_KEY, assetId)
        }
    }

    /** A layout length as the engine stores it. Value and mode travel together: a mode setter only swaps the unit. */
    private data class LayoutLength(
        val value: Float,
        val mode: SizeMode,
    )

    /**
     * A caption's laid-out width, or `null` when it is auto-sized and so has no box of its own to preserve.
     *
     * Resolved rather than raw: dragging a side handle rewrites the width in design units, so the value a
     * caption reports is only comparable across a preset apply once both ends are in the same unit.
     */
    private fun frameWidth(caption: DesignBlock): Float? {
        if (!isValid(caption)) return null
        val mode = runCatching { engine.block.getWidthMode(caption) }.getOrNull() ?: return null
        if (mode == SizeMode.AUTO) return null
        return runCatching { engine.block.getFrameWidth(caption) }.getOrNull()
    }

    /** A caption's height, as the value/mode pair [restoreCaptionBox] needs to put it back. */
    private fun layoutHeight(caption: DesignBlock): LayoutLength? = layoutLength(
        caption,
        mode = { engine.block.getHeightMode(it) },
        value = { engine.block.getHeight(it) },
    )

    private fun layoutLength(
        caption: DesignBlock,
        mode: (DesignBlock) -> SizeMode,
        value: (DesignBlock) -> Float,
    ): LayoutLength? {
        if (!isValid(caption)) return null
        val sizeMode = runCatching { mode(caption) }.getOrNull() ?: return null
        if (sizeMode == SizeMode.AUTO) return null
        val length = runCatching { value(caption) }.getOrNull() ?: return null
        return LayoutLength(value = length, mode = sizeMode)
    }

    /**
     * How much a caption has to be scaled to get back the width it had before a preset re-framed it, or `null`
     * when there is nothing to restore. Width alone drives it: the scale is uniform and the width is the axis
     * the typography follows.
     */
    private fun sizeRestoreRatio(
        caption: DesignBlock,
        previous: Float?,
    ): Float? {
        if (previous == null) return null
        val current = frameWidth(caption) ?: return null
        val ratio = previous / current
        // A zero or non-finite width would collapse the caption or trip an engine assert.
        if (!ratio.isFinite() || ratio <= 0F) return null
        return ratio
    }

    /**
     * Puts every caption on the track back on the box the user gave them, scaling each around its own top-left
     * corner, so size and placement both survive. Looping does not break the never-loop-a-style-setter rule:
     * `scale` writes values directly rather than through a synced property.
     */
    private fun restoreCaptionBox(
        caption: DesignBlock,
        ratio: Float,
        height: LayoutLength?,
    ) {
        val needsScale = abs(ratio - 1F) > SIZE_RESTORE_EPSILON
        // The ratio is width-driven, so a caption dragged taller but no wider needs this too.
        val needsHeight = height != null && height != layoutHeight(caption)
        if (!needsScale && !needsHeight) return
        if (needsScale) {
            captionsInTrack(caption).filter { isValid(it) }.forEach { target ->
                // Reported rather than dropped, and non-fatal: the preset itself has already been applied, so
                // giving up here would leave the caller's undo step unwritten.
                runCatching { engine.block.scale(target, ratio, anchorX = 0F, anchorY = 0F) }.onFailure(onError)
            }
        }
        // Once, after every scale: `scale` flushes pending syncs, so a height written inside the loop lands on
        // siblings that are not scaled yet. The outgoing sync carries it to them.
        if (height != null && isValid(caption)) {
            // Mode before value: the setter only swaps the unit.
            runCatching {
                engine.block.setHeightMode(caption, height.mode)
                engine.block.setHeight(caption, height.value)
            }.onFailure(onError)
        }
    }

    /** Every caption on the track holding [caption], falling back to the caption alone when it has no track. */
    private fun captionsInTrack(caption: DesignBlock): List<DesignBlock> {
        val track = runCatching { engine.block.getParent(caption) }.getOrNull() ?: return listOf(caption)
        return captionChildren(track).ifEmpty { listOf(caption) }
    }

    // endregion
    // region Private helpers

    /**
     * Centres a caption on its page. Aligning a *single* block registers an outgoing position sync, so this
     * re-places every caption on the track — which is why it may only run when there is no placement to keep.
     */
    private fun centerCaption(caption: DesignBlock) {
        engine.block.alignHorizontally(listOf(caption), HorizontalBlockAlignment.CENTER)
        engine.block.alignVertically(listOf(caption), VerticalBlockAlignment.CENTER)
    }

    private fun captionChildren(track: DesignBlock): List<DesignBlock> = runCatching {
        engine.block.getChildren(track).filter { isValid(it) && isCaption(it) }
    }.getOrDefault(emptyList())

    private fun currentPage(): DesignBlock? = runCatching { engine.getCurrentPage() }.getOrNull()

    private fun isValid(block: DesignBlock): Boolean = runCatching { engine.block.isValid(block) }.getOrDefault(false)

    private fun typeOf(block: DesignBlock): String? = runCatching { engine.block.getType(block) }.getOrNull()

    private fun isCaption(block: DesignBlock): Boolean = typeOf(block) == DesignBlockType.Caption.key

    private fun timeOffsetOf(block: DesignBlock): Double = runCatching { engine.block.getTimeOffset(block) }.getOrDefault(0.0)

    private fun durationOf(block: DesignBlock): Double = runCatching { engine.block.getDuration(block) }.getOrDefault(0.0)

    private fun findOrCreateCaptionTrack(page: DesignBlock): DesignBlock {
        captionTrack()?.let { return it }
        // A caption track initializes `automaticallyManageBlockOffsets = false` engine-side, which preserves the gaps between captions.
        val track = engine.block.create(DesignBlockType.CaptionTrack)
        engine.block.appendChild(page, track)
        return track
    }

    /** A time clamped into the page's timeline. */
    private fun clampToPage(
        seconds: Double,
        page: DesignBlock,
    ): Double {
        val pageDuration = durationOf(page)
        if (pageDuration <= 0.0) return seconds.coerceAtLeast(0.0)
        return seconds.coerceIn(0.0, pageDuration)
    }

    private fun rollBack(
        createdTrack: DesignBlock?,
        createdCaption: DesignBlock?,
    ) {
        if (createdTrack != null && isValid(createdTrack)) {
            runCatching { engine.block.destroy(createdTrack) }
        }
        if (createdCaption != null && isValid(createdCaption)) {
            runCatching { engine.block.destroy(createdCaption) }
        }
    }

    // endregion

    companion object {
        /**
         * The preset asset id held by a track's stamp.
         *
         * The stamp is the bare asset id. Scenes stamped by an earlier build carry a
         * `sourceId|assetId` pair, so the tail is taken — the preset source is always
         * [CAPTION_PRESETS_SOURCE_ID], which is why the leading half can be dropped rather than parsed.
         */
        fun stampedAssetId(stamp: String): String = stamp.substringAfterLast('|')
    }
}
