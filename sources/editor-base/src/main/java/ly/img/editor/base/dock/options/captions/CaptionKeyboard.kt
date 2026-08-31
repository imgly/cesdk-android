package ly.img.editor.base.dock.options.captions

/**
 * What a keystroke in a caption field should do.
 *
 * Return divides a caption, Backspace at the very start joins one back together.
 */
internal sealed interface CaptionKeyOperation {
    /** Not a structural key: let the text field apply it normally. */
    data object PassThrough : CaptionKeyOperation

    /** A structural key with nothing to do here. Consumed, so it doesn't also type a character. */
    data object Ignore : CaptionKeyOperation

    /** Divide the caption at this UTF-16 offset; the text after it moves to a new caption below. */
    data class SplitAt(
        val utf16Offset: Int,
    ) : CaptionKeyOperation

    /** Append a new, empty caption after this one and edit it. */
    data object AddCaptionAfter : CaptionKeyOperation

    /** Join this caption into the one above it. */
    data object MergeWithPrevious : CaptionKeyOperation

    /** Remove this caption. */
    data object DeleteCaption : CaptionKeyOperation
}

/** The only two keys a caption field treats as structural. */
internal enum class CaptionKey {
    Return,
    Backspace,
}

/**
 * What a change reported by the text field means, and the text to act on.
 *
 * [text] carries the new contents with the typed newline removed — the input method may have corrected the
 * text in the same change, so the previous text is already stale.
 */
internal data class CaptionValueChange(
    val operation: CaptionKeyOperation,
    val text: String,
) {
    companion object {
        val PassThrough = CaptionValueChange(CaptionKeyOperation.PassThrough, text = "")
    }
}

/** A caption field's contents at one instant. Offsets are UTF-16, directly comparable with [text].length. */
internal data class CaptionFieldValue(
    val text: String,
    val selectionStart: Int,
    val selectionEnd: Int,
    /** Whether an input method is mid-composition (CJK, dictation, an autocorrect suggestion). */
    val isComposing: Boolean,
) {
    /** Where a keystroke would land: the caret, or the start of the range it would replace. */
    val caret: Int get() = selectionStart

    val hasSelection: Boolean get() = selectionStart != selectionEnd

    /** How many line breaks the selected range covers; a keystroke replacing it removes them all. */
    fun newlinesInSelection(): Int {
        if (!hasSelection) return 0
        val start = minOf(selectionStart, selectionEnd).coerceIn(0, text.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(start, text.length)
        return text.substring(start, end).count { it == '\n' }
    }
}

/**
 * The keyboard semantics of a caption field, as pure decisions.
 *
 * Two entry points because Android surfaces the keys through two channels: [keyOperation] for key events,
 * [valueChangeOperation] for the bare `\n` a soft keyboard commits instead of a Return key event.
 *
 * Backspace is deliberately not recognised from a value change: a no-op delete at offset 0 reports a value
 * identical to a mere caret placement, and guessing wrong would destroy a caption on a stray tap.
 */
internal object CaptionKeyboard {
    private const val NEWLINE = '\n'

    /** Decides what a key event means; the caller matches the physical key and passes it as [key]. */
    fun keyOperation(
        key: CaptionKey,
        value: CaptionFieldValue,
        hasPreviousCaption: Boolean,
    ): CaptionKeyOperation = when (key) {
        CaptionKey.Return -> returnOperation(
            caret = value.caret,
            textLength = value.text.length,
            isComposing = value.isComposing,
        )
        CaptionKey.Backspace -> backspaceOperation(
            caret = value.caret,
            hasSelection = value.hasSelection,
            isTextEmpty = value.text.isEmpty(),
            hasPreviousCaption = hasPreviousCaption,
        )
    }

    /**
     * Decides what a transition reported by `onValueChange` means.
     *
     * Only a bare Return is structural; typing, multi-line pastes and composition commits are left alone.
     */
    fun valueChangeOperation(
        old: CaptionFieldValue,
        new: CaptionFieldValue,
    ): CaptionValueChange {
        val caret = new.caret - 1
        if (caret < 0 || new.text.getOrNull(caret) != NEWLINE) return CaptionValueChange.PassThrough
        // Requiring the change to add exactly one newline separates the keystroke from a multi-line paste. Newlines
        // the change replaced are discounted, so a Return over a selection spanning one still reads as a Return.
        val expected = old.text.count { it == NEWLINE } - old.newlinesInSelection() + 1
        if (new.text.count { it == NEWLINE } != expected) return CaptionValueChange.PassThrough

        // Stripping the newline from the new text rather than reusing the old keeps edits the IME made in the same change.
        // Composition state is deliberately not consulted: IMEs carry a composing region across the Return in both directions.
        val text = new.text.removeRange(caret, caret + 1)
        val operation = returnOperation(caret = caret, textLength = text.length, isComposing = false)
        return CaptionValueChange(operation = operation, text = text)
    }

    /** Decides what a Return means. A caption never holds a typed line break. */
    fun returnOperation(
        caret: Int,
        textLength: Int,
        isComposing: Boolean,
    ): CaptionKeyOperation = when {
        isComposing -> CaptionKeyOperation.PassThrough
        caret >= textLength -> CaptionKeyOperation.AddCaptionAfter
        // Splitting at the very start would leave an empty caption above.
        caret <= 0 -> CaptionKeyOperation.Ignore
        else -> CaptionKeyOperation.SplitAt(caret)
    }

    /** Decides what a Backspace means. Only a collapsed caret at the very start is structural. */
    fun backspaceOperation(
        caret: Int,
        hasSelection: Boolean,
        isTextEmpty: Boolean,
        hasPreviousCaption: Boolean,
    ): CaptionKeyOperation = when {
        hasSelection || caret != 0 -> CaptionKeyOperation.PassThrough
        isTextEmpty -> CaptionKeyOperation.DeleteCaption
        // The first caption has nothing above it, so the key does nothing rather than silently deleting text.
        hasPreviousCaption -> CaptionKeyOperation.MergeWithPrevious
        else -> CaptionKeyOperation.PassThrough
    }
}
