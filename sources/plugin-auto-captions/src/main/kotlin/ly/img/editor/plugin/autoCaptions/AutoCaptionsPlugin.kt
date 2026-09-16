package ly.img.editor.plugin.autoCaptions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ly.img.editor.core.configuration.EditorConfiguration
import ly.img.editor.core.configuration.EditorConfigurationBuilder
import java.io.File

/**
 * Transcribes the scene's audible content into an SRT or VTT file, with cue timings relative to the page timeline,
 * or null when the audio holds no speech.
 *
 * Lives here, not in the editor: the editor reads the callback out of its state by key.
 */
typealias CaptionsGenerator = suspend () -> File?

/** The key the generator is published under. Duplicated on the editor side — keep both in step. */
private const val GENERATOR_KEY = "ly.img.editor.plugin.autoCaptions.generator"

/**
 * Plugin for automatic caption generation.
 *
 * Transcribes the scene's audible content via [provider] into styled, time-synced captions, and publishes that
 * capability for the editor to offer.
 *
 * ```kotlin
 * EditorConfiguration.remember(::VideoConfigurationBuilder) {
 *     dock = { ... }
 * }.then(::AutoCaptionsPlugin) {
 *     provider = GatewayTranscriptionProvider(apiKey = "sk_…")
 * }
 * ```
 */
open class AutoCaptionsPlugin : EditorConfigurationBuilder() {
    /** The speech-to-text backend, e.g. [ly.img.editor.plugin.autoCaptions.gateway.GatewayTranscriptionProvider]. */
    var provider by editorContext.mutableStateOf<TranscriptionProvider?>(
        key = "ly.img.editor.plugin.autoCaptions.provider",
        initial = null,
    )

    /** Language and subtitle formatting options passed to [provider]. */
    var options by editorContext.mutableStateOf<TranscriptionOptions>(
        key = "ly.img.editor.plugin.autoCaptions.options",
        initial = TranscriptionOptions(),
    )

    /**
     * Transcribes the scene's audible content into an SRT file. Replace it to source the captions differently
     * while keeping the rest of the plugin.
     *
     * [provider] stays required even when replaced — the plugin fails at setup without one.
     */
    var captionsGeneration: CaptionsGenerator = {
        AutoCaptionsGenerator.generateCaptionsFile(
            engine = editorContext.engine,
            // `cacheDir` stats the directory, so it is a disk read like any other.
            cacheDir = withContext(Dispatchers.IO) { editorContext.activity.cacheDir },
            provider = requireProvider(),
            options = options,
        )
    }

    /**
     * Fails at setup when no provider is configured, rather than as a generic error on first use, and publishes
     * [captionsGeneration] for the editor, which cannot depend on this optional plugin. Runs last, so it picks up
     * a [captionsGeneration] the integrator replaced.
     */
    override var decorator: @Composable (EditorConfiguration.() -> EditorConfiguration) = {
        requireProvider()
        generatorState.value = captionsGeneration
        this
    }

    private val generatorState = editorContext.mutableStateOf<CaptionsGenerator?>(
        key = GENERATOR_KEY,
        initial = null,
    )

    private fun requireProvider() = requireNotNull(provider) {
        "Configure the \"provider\" property inside the AutoCaptionsPlugin configuration block."
    }
}
