package ly.img.editor.base.dock.options.captions

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ly.img.engine.DesignBlock
import java.io.File

/** Keeps one [CaptionsEditingController] alive for as long as the sheet's engine and scope are. */
@Composable
internal fun rememberCaptionsEditingController(
    captionsEngine: CaptionsEngine,
    coroutineScope: CoroutineScope,
    generationScope: CoroutineScope,
    generationJobState: MutableState<Job?>,
    clearFocus: () -> Unit,
): CaptionsEditingController = remember(captionsEngine, coroutineScope) {
    CaptionsEditingController(captionsEngine, coroutineScope, generationScope, generationJobState, clearFocus)
}

/**
 * The captions sheet's editing state and the operations the list, the rows and the keyboard action bar run through.
 *
 * Only one caption is edited at a time, so the in-flight text lives here as a single value — an unfocused row always mirrors the engine,
 * which stops a stale row writing its text over an operation's result. Text is committed on blur and before every structural operation,
 * never per keystroke, so typing doesn't fill the undo history.
 */
internal class CaptionsEditingController(
    private val captionsEngine: CaptionsEngine,
    private val coroutineScope: CoroutineScope,
    /** Outlives the sheet, so a transcription is not thrown away by a glance at the timeline. */
    private val generationScope: CoroutineScope,
    /** Editor-scoped too, so a reopened sheet finds a generation still running. */
    private val generationJobState: MutableState<Job?>,
    /** Drops Compose focus, which also dismisses the keyboard. */
    private val clearFocus: () -> Unit,
) {
    /** Bumped after every mutation so the sheet re-reads the captions and the rows re-read their text. */
    var refreshToken by mutableIntStateOf(0)
        private set

    /** The caption that owns the keyboard, or `null` when nothing is being edited. */
    var editingCaption by mutableStateOf<DesignBlock?>(null)
        private set

    /** The caption with the accent ring — the canvas selection the sheet drives. Editing implies selected. */
    var selectedCaption by mutableStateOf<DesignBlock?>(null)
        private set

    /** The in-flight value of the caption being edited. */
    var editingValue by mutableStateOf<TextFieldValue?>(null)
        private set

    /**
     * Counts the rewrites of [editingValue] this controller made itself.
     *
     * The field diffs against what it last reported, so a rewrite that only moves the caret is invisible to it.
     * Bumping this tells it to adopt the new value; without it a Backspace straight after a split or merge would
     * be judged against the caret the input method last sent, and quietly do nothing.
     */
    var editingValueRevision by mutableIntStateOf(0)
        private set

    /** True while an operation that suspends on asset I/O is running, so nothing else can race it. */
    var isMutating by mutableStateOf(false)
        private set

    /** True while a picked caption file is being read and imported, so the Import action can show its progress. */
    var isImporting by mutableStateOf(false)
        private set

    /** The in-flight generation; non-null swaps the Add state for the generating view. */
    var generationJob by generationJobState
        private set

    /** The engine text the edited row started from, so an external rewrite can be told from live typing. */
    private var loadedText: String = ""

    /** Where the caret belongs when a caption takes focus — the tail of a split opens at the cut. */
    private var pendingCaret: PendingCaret? = null

    /** One requester per caption, so focus can be handed to a row before its neighbour is torn down. */
    private val focusRequesters = mutableMapOf<DesignBlock, FocusRequester>()

    /**
     * Derived rather than read straight through: the sheet recomposes on every frame the keyboard animates over,
     * and each of those frames would otherwise walk the caption track in the engine.
     */
    val captions: List<DesignBlock> by derivedStateOf {
        @Suppress("UNUSED_EXPRESSION")
        refreshToken // establishes the dependency that re-reads after a mutation
        captionsEngine.captions()
    }

    fun focusRequester(caption: DesignBlock): FocusRequester = focusRequesters.getOrPut(caption) { FocusRequester() }

    fun textOf(caption: DesignBlock): String = captionsEngine.text(caption)

    // region Refresh

    /** Re-reads the captions and drops references to blocks an undo or a merge destroyed. */
    fun refresh() {
        refreshToken++
        val current = captionsEngine.captions()
        if (selectedCaption !in current) selectedCaption = null
        if (editingCaption !in current) {
            editingCaption = null
            editingValue = null
        }
        focusRequesters.keys.retainAll(current.toSet())
    }

    /** Re-seeds the edited row when something other than its own typing rewrote the caption; the diff is against the last read text. */
    fun reloadEditingValueIfChanged() {
        val caption = editingCaption ?: return
        val engineText = captionsEngine.text(caption)
        if (engineText == loadedText) return
        seedEditingValue(caption, caret = null)
    }

    // endregion
    // region Focus and selection

    /** Handles a row reporting a focus change; either ordering of a row-to-row handover commits the outgoing text exactly once. */
    fun onRowFocusChanged(
        caption: DesignBlock,
        isFocused: Boolean,
    ) {
        if (isFocused) {
            if (editingCaption == caption) {
                // Re-assert the caret on focus: the input session that starts here otherwise moves it to the end of the text.
                applyPendingCaret(caption)
                return
            }
            commitEditingText()
            beginEditing(caption)
        } else if (editingCaption == caption) {
            commitEditingText()
            editingCaption = null
            editingValue = null
            // `pendingCaret` deliberately survives a blur: a row recomposed by the list can blur and refocus in the
            // middle of a hand-over, and dropping the request here loses the caret the split or merge asked for.
        }
    }

    /** Selects a caption without editing it: accent ring, canvas selection, paused playhead seek. */
    fun select(caption: DesignBlock) {
        selectedCaption = caption
        captionsEngine.revealCaption(caption)
    }

    /**
     * Hands the keyboard to a caption, placing the caret at [caret] once the row has it.
     *
     * Focus is taken in this pass, not a later frame: a turn with no focused field reads as the keyboard closing and reopening.
     */
    fun focusCaption(
        caption: DesignBlock,
        caret: Int? = null,
    ) {
        pendingCaret = caret?.let { PendingCaret(caption = caption, offset = it) }
        // Assign first so the retry effect has a target even when the row isn't on screen yet.
        beginEditing(caption)
        requestFocus(caption)
    }

    /** Requests focus for a caption's row, reporting whether the row was there to take it. */
    fun requestFocus(caption: DesignBlock): Boolean {
        val requester = focusRequesters[caption] ?: return false
        // `requestFocus` throws when the node isn't attached, which is exactly the "not composed yet" case.
        return runCatching { requester.requestFocus() }.isSuccess
    }

    /**
     * Gives up the keyboard while keeping the caption selected.
     *
     * For the cases where the keyboard goes away for a reason other than the user being finished with the caption — the
     * system took it, or a dialog is about to cover it — so the accent ring and canvas selection survive.
     *
     * The text is committed here rather than on blur, which may never arrive. Focus must be dropped explicitly: the
     * controls that end editing aren't focusable in touch mode, and a field that keeps focus while nothing is edited
     * never reports a focus change again, so it can't be re-entered.
     */
    fun dismissKeyboard() {
        commitEditingText()
        editingCaption = null
        editingValue = null
        pendingCaret = null
        clearFocus()
    }

    /** Ends editing outright — the user is done with the caption, so it stops being the selection too. */
    fun endEditing() {
        dismissKeyboard()
        selectedCaption = null
    }

    private fun beginEditing(caption: DesignBlock) {
        editingCaption = caption
        // A request aimed at another row is stale — a merge leaves one behind that no focus change ever spends.
        if (pendingCaret?.caption != caption) pendingCaret = null
        // The request survives seeding; it is spent again once the row reports focus.
        seedEditingValue(caption, caret = pendingCaret?.offset)
        select(caption)
    }

    /** Applies the requested caret once the row owns the keyboard. Spent exactly once. */
    private fun applyPendingCaret(caption: DesignBlock) {
        val caret = pendingCaret?.takeIf { it.caption == caption } ?: return
        pendingCaret = null
        val value = editingValue ?: return
        editingValue = value.copy(selection = TextRange(caret.offset.coerceIn(0, value.text.length)))
        editingValueRevision++
    }

    /** Loads a caption's engine text into the field, recording what was read so a later divergence shows up. */
    private fun seedEditingValue(
        caption: DesignBlock,
        caret: Int?,
    ) {
        val text = captionsEngine.text(caption)
        loadedText = text
        editingValue = TextFieldValue(text = text, selection = TextRange((caret ?: text.length).coerceIn(0, text.length)))
        editingValueRevision++
    }

    /** Takes a row's new value. Only the edited row's is kept — a blurred row can still report a change while the IME catches up. */
    fun onRowValueChange(
        caption: DesignBlock,
        value: TextFieldValue,
    ) {
        if (editingCaption != caption) return
        editingValue = value
    }

    // endregion
    // region Text

    /** Persists the edited row's text. Every structural operation calls this first so it acts on what is on screen. */
    fun commitEditingText() {
        val caption = editingCaption ?: return
        val text = editingValue?.text ?: return
        if (captionsEngine.text(caption) == text) return
        captionsEngine.setText(text, caption)
        loadedText = text
        // The rows cache their engine text against this token, so it has to move with the write.
        refreshToken++
    }

    // endregion
    // region Structural operations

    /** Creates a caption at the playhead and starts editing it. */
    fun addCaption() {
        runMutating {
            commitEditingText()
            val new = captionsEngine.createCaption()
            refresh()
            new?.let { focusCaption(it) }
        }
    }

    /**
     * Replaces every caption with the ones parsed from a picked SRT or VTT file.
     *
     * @param stageFile copies the picked file somewhere the engine can read and classify it; the copy is
     * deleted again however the import ends.
     * @param onFailure reports a failure the user should see. The scene is untouched when it fires.
     */
    fun importCaptions(
        stageFile: suspend () -> File,
        onFailure: (Throwable) -> Unit,
    ) {
        runMutating {
            isImporting = true
            var staged: File? = null
            try {
                // An import does not stop half way. The engine parses the whole file before it hands the caption
                // blocks back, so a cancellation landing in the middle — closing the sheet, rotating the device —
                // would leave those blocks created with nobody holding their ids to attach or destroy them. Running
                // the step to completion keeps the outcome binary: the captions are imported, or nothing changed.
                withContext(NonCancellable) {
                    commitEditingText()
                    val file = stageFile().also { staged = it }
                    captionsEngine.importCaptions(Uri.fromFile(file).toString())
                }
                refresh()
            } catch (cancellation: CancellationException) {
                // Only reachable if the scope is already cancelled when this body starts, since the import itself
                // runs uncancellable. Rethrown either way: it is not a failure to report, and swallowing it would
                // leave the coroutine machinery believing the job completed.
                throw cancellation
            } catch (throwable: Throwable) {
                onFailure(throwable)
            } finally {
                // Off the main thread — deleting a file is disk I/O, and NonCancellable so the copy is still
                // cleaned up when the sheet closes mid-import and cancels this scope.
                staged?.let { file ->
                    withContext(NonCancellable + Dispatchers.IO) { file.delete() }
                }
                isImporting = false
            }
        }
    }

    /**
     * Transcribes the scene into captions, importing the result through the same pipeline as a file import so
     * styling, track replacement and the single undo step behave identically.
     *
     * Runs on the editor's scope rather than the sheet's: dismissing the sheet to glance at the timeline must not
     * throw a long transcription away. Reopening finds it still running. Nothing reaches the scene until [generate]
     * returns, which is what makes [cancelGeneration] side-effect free.
     *
     * @param generate produces the captions file, or null when the audio held nothing to transcribe; the editor
     * deletes the file however the run ends.
     * @param onNoSpeech the audio held nothing to transcribe.
     * @param onFailure any other failure the user should see. The scene is untouched when either fires.
     */
    fun generateCaptions(
        generate: suspend () -> File?,
        onNoSpeech: () -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        if (isMutating || generationJob != null) return
        isMutating = true
        // Lazy: with `Main.immediate`, a non-suspending `generate` would clear `generationJob` before it is assigned.
        val job = generationScope.launch(start = CoroutineStart.LAZY) {
            var generated: File? = null
            try {
                val file = generate()?.also { generated = it }
                if (file == null) {
                    onNoSpeech()
                    return@launch
                }
                // Uncancellable for the same reason an import is; a cancel that raced it is undone below.
                withContext(NonCancellable) {
                    commitEditingText()
                    captionsEngine.importCaptions(Uri.fromFile(file).toString())
                }
                if (!isActive) captionsEngine.revertLastStep()
                refresh()
            } catch (cancellation: CancellationException) {
                // Cancelled — fall back to the Add state silently.
            } catch (throwable: Throwable) {
                // A cancelled network call surfaces as its transport's error, not a CancellationException.
                // The dialog only says that it failed, so the cause goes to the integrator's `onError` too.
                if (isActive) onFailure(throwable)
            } finally {
                generated?.let { file ->
                    withContext(NonCancellable + Dispatchers.IO) { file.delete() }
                }
                generationJob = null
                isMutating = false
            }
        }
        generationJob = job
        job.start()
    }

    /** Cancels an in-flight generation, returning the sheet to its Add state with the scene untouched. */
    fun cancelGeneration() {
        generationJob?.cancel()
    }

    /** Inserts a caption after [caption] and starts editing it. */
    fun addCaptionAfter(caption: DesignBlock) {
        runMutating {
            commitEditingText()
            val new = captionsEngine.addCaptionAfter(caption)
            refresh()
            new?.let { focusCaption(it) }
        }
    }

    /**
     * Joins a caption into the one above it.
     *
     * The edited caption absorbs its predecessor so the focused field is never torn down; from a row's menu the predecessor absorbs it
     * instead, so the list collapses in place.
     */
    fun mergeWithPrevious(caption: DesignBlock) {
        if (isMutating) return
        val siblings = captionsEngine.captions()
        val index = siblings.indexOf(caption)
        if (index <= 0) return
        val isEditingThis = editingCaption == caption
        commitEditingText()
        val mergedCaret = CaptionTiming.mergedCaret(
            previousText = captionsEngine.text(siblings[index - 1]),
            currentText = captionsEngine.text(caption),
            caretInCurrent = if (isEditingThis) editingValue?.selection?.min ?: 0 else 0,
        )
        val survivor = captionsEngine.mergeWithPrevious(caption, keepingCurrent = isEditingThis) ?: return
        refresh()
        if (isEditingThis) {
            pendingCaret = PendingCaret(caption = survivor, offset = mergedCaret)
            beginEditing(survivor)
            // Focus never left this row, so no focus change will spend the request; seeding already applied it.
            pendingCaret = null
        } else if (selectedCaption == caption || selectedCaption == siblings[index - 1]) {
            // Only carry the selection over when one of the merged rows already had it.
            selectedCaption = survivor
            captionsEngine.revealCaption(survivor)
        }
    }

    /** Divides the edited caption at the caret and carries on in the tail; a caret at the end appends instead of splitting. */
    fun splitAtCaret(
        caption: DesignBlock,
        caretUtf16: Int,
    ) {
        if (isMutating) return
        commitEditingText()
        if (caretUtf16 >= captionsEngine.text(caption).length) {
            addCaptionAfter(caption)
            return
        }
        val new = captionsEngine.splitCaption(caption, caretUtf16) ?: return
        refresh()
        focusCaption(new, caret = 0)
    }

    /**
     * Deletes a caption and hands the keyboard to the row below it, falling back to the one above.
     *
     * Focus moves *before* the block is destroyed; the other order tears the focused field down with the row and drops the keyboard.
     */
    fun deleteCaption(caption: DesignBlock) {
        if (isMutating) return
        // A field's text is local to its row until it is written back, so deleting without committing hands
        // undo the older text — of this caption, or of whichever row was being edited when a swipe deleted
        // another one.
        commitEditingText()
        val wasEditing = editingCaption == caption
        val successor = if (wasEditing) successorOf(caption) else null
        if (wasEditing) {
            if (successor != null) {
                focusCaption(successor.caption, caret = successor.caret)
            } else {
                editingCaption = null
                editingValue = null
                clearFocus()
            }
        }
        if (selectedCaption == caption) selectedCaption = null
        captionsEngine.deleteCaption(caption)
        refresh()
    }

    /** Discards every caption and returns the sheet to its Add state. */
    fun deleteAllCaptions() {
        if (isMutating) return
        editingCaption = null
        editingValue = null
        selectedCaption = null
        clearFocus()
        captionsEngine.deleteAllCaptions()
        refresh()
    }

    /** Moves the keyboard [step] rows, committing the current text first so stepping off never drops an edit. */
    fun focusRelative(step: Int) {
        if (isMutating) return
        val caption = editingCaption ?: return
        val siblings = captionsEngine.captions()
        val target = siblings.getOrNull(siblings.indexOf(caption) + step) ?: return
        commitEditingText()
        focusCaption(target)
    }

    /** Runs a structural key operation against a caption. Returns whether it ran, so the key is consumed. */
    fun runKeyOperation(
        operation: CaptionKeyOperation,
        caption: DesignBlock,
        liveText: String,
    ): Boolean {
        // Anything but the focused row is a stale callback racing a focus handover, and would act on the wrong caption.
        if (isMutating || editingCaption != caption) return false
        // `liveText` is the text the keystroke acted on, which can be ahead of the controller's copy when the IME edited it in the
        // same change (an autocorrection confirmed by the Return).
        editingValue = editingValue?.copy(text = liveText) ?: TextFieldValue(liveText)
        editingValueRevision++
        return when (operation) {
            is CaptionKeyOperation.SplitAt -> {
                splitAtCaret(caption, operation.utf16Offset)
                true
            }
            CaptionKeyOperation.AddCaptionAfter -> {
                addCaptionAfter(caption)
                true
            }
            CaptionKeyOperation.MergeWithPrevious -> {
                mergeWithPrevious(caption)
                true
            }
            CaptionKeyOperation.DeleteCaption -> {
                // Backspace carries on at the end of the caption above; the toolbar's delete deliberately reads downwards instead.
                val previous = previousOf(caption)
                if (previous != null) {
                    focusCaption(previous, caret = captionsEngine.text(previous).length)
                } else {
                    editingCaption = null
                    editingValue = null
                    clearFocus()
                }
                if (selectedCaption == caption) selectedCaption = null
                captionsEngine.deleteCaption(caption)
                refresh()
                true
            }
            CaptionKeyOperation.Ignore, CaptionKeyOperation.PassThrough -> false
        }
    }

    // endregion
    // region Private helpers

    private fun previousOf(caption: DesignBlock): DesignBlock? {
        val siblings = captionsEngine.captions()
        val index = siblings.indexOf(caption)
        return if (index > 0) siblings[index - 1] else null
    }

    /** The caption that should take the keyboard once [caption] is deleted, and where its caret belongs. */
    private fun successorOf(caption: DesignBlock): Successor? {
        val siblings = captionsEngine.captions()
        val index = siblings.indexOf(caption)
        if (index < 0) return null
        siblings.getOrNull(index + 1)?.let { return Successor(it, caret = 0) }
        val previous = siblings.getOrNull(index - 1) ?: return null
        return Successor(previous, caret = captionsEngine.text(previous).length)
    }

    private fun runMutating(block: suspend () -> Unit) {
        if (isMutating) return
        isMutating = true
        coroutineScope.launch {
            try {
                block()
            } finally {
                isMutating = false
            }
        }
    }

    private data class Successor(
        val caption: DesignBlock,
        val caret: Int,
    )

    // endregion
}

/** A caret placement waiting for its caption to take focus. Keyed by caption so a stale request cannot land on another row. */
private data class PendingCaret(
    val caption: DesignBlock,
    val offset: Int,
)
