package ly.img.editor.plugin.backgroundRemoval.util

import android.util.Log
import kotlin.time.measureTimedValue

internal inline fun <T> measureAndGet(
    step: String,
    block: () -> T,
): T {
    if (BackgroundRemovalConstants.isDebug.not()) return block()
    return measureTimedValue(block).let {
        Log.d(BackgroundRemovalConstants.TAG, "step = $step, duration = ${it.duration}")
        it.value
    }
}
