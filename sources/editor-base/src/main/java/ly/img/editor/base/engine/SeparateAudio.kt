package ly.img.editor.base.engine

import ly.img.editor.core.ui.engine.getFillType
import ly.img.engine.AudioFromVideoOptions
import ly.img.engine.DesignBlock
import ly.img.engine.Engine
import ly.img.engine.FillType

/**
 * Moves the audio of [clip] into one new audio block per audio track and mutes [clip]. The audio blocks start with
 * [clip], play like it and are named after it, or [fallbackName] when it has no name. The audio of a background clip
 * goes into the audio rows directly above the background track, the audio of an overlay into the top audio rows.
 * The first track goes on top. The change is one undo step.
 *
 * @param clip the video clip whose audio is separated.
 * @param fallbackName the name of the audio blocks when [clip] has no name.
 * @return the new audio blocks, an empty list when the video has no audio, or null when [clip] shows no video.
 */
suspend fun Engine.separateAudio(
    clip: DesignBlock,
    fallbackName: String,
): List<DesignBlock>? {
    val page = scene.getCurrentPage() ?: return null
    if (block.getFillType(clip) != FillType.Video) return null
    val fill = block.getFill(clip)
    // The audio tracks of a video are known only after it loads.
    block.forceLoadAVResource(fill)
    // The clip can be deleted while its video loads.
    if (!block.isValid(clip)) return null
    if (block.getAudioTrackCountFromVideo(fill) == 0) return emptyList()

    // The timeline shows the audio rows between the overlays and the background track, and the audio block with the
    // lower page index in the higher row.
    val topAudioRowIndex = if (block.isParentBackgroundTrack(clip)) {
        null
    } else {
        block.getChildren(page).indexOfFirst(block::containsAudio).takeIf { it >= 0 }
    }
    val audioBlocks = block.createAudiosFromVideo(
        videoFill = fill,
        options = AudioFromVideoOptions(keepTrimSettings = true, muteOriginalVideo = true),
    )
    val timeOffset = block.getTimeOffset(clip)
    val clipName = if (block.hasMetadata(clip, NAME_METADATA_KEY)) block.getMetadata(clip, NAME_METADATA_KEY) else ""
    // A video from the gallery has no name.
    val audioName = clipName.ifEmpty { fallbackName }
    audioBlocks.forEachIndexed { index, audioBlock ->
        if (topAudioRowIndex != null) {
            block.insertChild(parent = page, child = audioBlock, index = topAudioRowIndex + index)
        } else {
            block.appendChild(parent = page, child = audioBlock)
        }
        // Keeps the audio out of the z-index reordering, like an audio block added from the library.
        block.setAlwaysOnTop(audioBlock, true)
        block.setTimeOffset(audioBlock, timeOffset)
        // The engine names the block after its audio track, and the web timeline shows that name first.
        block.setName(audioBlock, "")
        val name = if (audioBlocks.size > 1) "$audioName ${index + 1}" else audioName
        block.setMetadata(audioBlock, NAME_METADATA_KEY, name)
    }
    editor.addUndoStep()
    return audioBlocks
}

private const val NAME_METADATA_KEY = "name"
