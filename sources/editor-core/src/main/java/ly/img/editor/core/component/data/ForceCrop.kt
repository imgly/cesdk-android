package ly.img.editor.core.component.data

/**
 * Configuration for applying a force crop preset.
 *
 * @param sourceId id of the asset source that contains the crop preset.
 * @param presetId id of the crop preset that should be applied.
 * @param mode the mode that controls how the crop preset is applied.
 * @param presetCandidates the list of crop presets that can be applied.
 */
data class ForceCropConfiguration(
    val sourceId: String = "",
    val presetId: String = "",
    val mode: ForceCropMode = ForceCropMode.Silent,
    val presetCandidates: List<ForceCropPresetCandidate> = emptyList(),
)

/**
 * Defines how the force crop preset should be applied.
 */
sealed class ForceCropMode {
    /** Applies the preset silently without opening the crop UI. */
    data object Silent : ForceCropMode()

    /** Applies the preset and always opens the crop UI afterwards. */
    data object Always : ForceCropMode()

    /**
     * Applies the preset only if needed. When applied the crop UI is opened.
     *
     * @param threshold The allowed difference when comparing the current dimensions with the preset.
     */
    data class IfNeeded(
        val threshold: Float = 0.0001F,
    ) : ForceCropMode()
}

/**
 * Candidate crop preset that can be applied in force crop.
 *
 * @param sourceId id of the asset source that contains the crop preset.
 * @param presetId id of the crop preset that should be applied.
 */
data class ForceCropPresetCandidate(
    val sourceId: String,
    val presetId: String,
)
