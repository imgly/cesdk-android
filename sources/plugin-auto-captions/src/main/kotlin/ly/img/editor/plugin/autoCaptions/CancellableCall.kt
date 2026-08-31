package ly.img.editor.plugin.autoCaptions

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Response
import java.io.IOException

/**
 * Runs [block] with this call's response, aborting the call as soon as the caller is cancelled.
 *
 * A completion handler on the job that issues the request is too late: it fires when that job reaches its *final*
 * state, which a blocking `execute()` or a blocking body read prevents until the request has finished on its own.
 * A child coroutine parked on [awaitCancellation] has nothing to finish, so it completes the moment cancellation is
 * requested — early enough for the abort to interrupt a transcription that would otherwise run to its timeout.
 */
internal suspend fun <T> Call.useCancellable(block: suspend (Response) -> T): T = coroutineScope {
    val call = this@useCancellable
    val aborter = launch(Dispatchers.Default) {
        try {
            awaitCancellation()
        } finally {
            // A no-op once the call has completed, so the success path is unaffected.
            call.cancel()
        }
    }
    try {
        val response = try {
            withContext(Dispatchers.IO) { call.execute() }
        } catch (throwable: IOException) {
            // An aborted call surfaces as an opaque IO error; report the cancellation itself instead.
            if (!isActive) throw CancellationException("Cancelled")
            throw throwable
        }
        response.use { block(it) }
    } finally {
        aborter.cancel()
    }
}
