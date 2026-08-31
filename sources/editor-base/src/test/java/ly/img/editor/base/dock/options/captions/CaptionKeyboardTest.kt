package ly.img.editor.base.dock.options.captions

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the caption keyboard semantics without an input method on screen. Every case here was reached by
 * trial and error, so a change that "looks harmless" should have to break a test first.
 */
class CaptionKeyboardTest {
    // region Return

    @Test
    fun `return in the middle of the text splits at the caret`() {
        assertEquals(
            CaptionKeyOperation.SplitAt(5),
            CaptionKeyboard.returnOperation(caret = 5, textLength = 11, isComposing = false),
        )
    }

    @Test
    fun `return at the end of the text adds a caption after`() {
        assertEquals(
            CaptionKeyOperation.AddCaptionAfter,
            CaptionKeyboard.returnOperation(caret = 11, textLength = 11, isComposing = false),
        )
    }

    @Test
    fun `return in an empty caption adds a caption after`() {
        assertEquals(
            CaptionKeyOperation.AddCaptionAfter,
            CaptionKeyboard.returnOperation(caret = 0, textLength = 0, isComposing = false),
        )
    }

    @Test
    fun `return at the very start of a non-empty caption is consumed and ignored`() {
        assertEquals(
            CaptionKeyOperation.Ignore,
            CaptionKeyboard.returnOperation(caret = 0, textLength = 11, isComposing = false),
        )
    }

    @Test
    fun `return during composition passes through`() {
        assertEquals(
            CaptionKeyOperation.PassThrough,
            CaptionKeyboard.returnOperation(caret = 5, textLength = 11, isComposing = true),
        )
    }

    // endregion
    // region Backspace

    @Test
    fun `backspace at the start of a non-empty caption merges with the previous one`() {
        assertEquals(
            CaptionKeyOperation.MergeWithPrevious,
            CaptionKeyboard.backspaceOperation(
                caret = 0,
                hasSelection = false,
                isTextEmpty = false,
                hasPreviousCaption = true,
            ),
        )
    }

    @Test
    fun `backspace at the start of the first caption passes through`() {
        assertEquals(
            CaptionKeyOperation.PassThrough,
            CaptionKeyboard.backspaceOperation(
                caret = 0,
                hasSelection = false,
                isTextEmpty = false,
                hasPreviousCaption = false,
            ),
        )
    }

    @Test
    fun `backspace in an empty caption deletes it, even when it is the first one`() {
        assertEquals(
            CaptionKeyOperation.DeleteCaption,
            CaptionKeyboard.backspaceOperation(
                caret = 0,
                hasSelection = false,
                isTextEmpty = true,
                hasPreviousCaption = false,
            ),
        )
    }

    @Test
    fun `backspace away from the start passes through`() {
        assertEquals(
            CaptionKeyOperation.PassThrough,
            CaptionKeyboard.backspaceOperation(
                caret = 1,
                hasSelection = false,
                isTextEmpty = false,
                hasPreviousCaption = true,
            ),
        )
    }

    @Test
    fun `backspace over a selection passes through`() {
        assertEquals(
            CaptionKeyOperation.PassThrough,
            CaptionKeyboard.backspaceOperation(
                caret = 0,
                hasSelection = true,
                isTextEmpty = false,
                hasPreviousCaption = true,
            ),
        )
    }

    // endregion
    // region Key events

    @Test
    fun `key events route through the same rules as the raw decisions`() {
        val value = value(text = "Hello world", caret = 5)
        assertEquals(
            CaptionKeyOperation.SplitAt(5),
            CaptionKeyboard.keyOperation(CaptionKey.Return, value, hasPreviousCaption = true),
        )
        assertEquals(
            CaptionKeyOperation.MergeWithPrevious,
            CaptionKeyboard.keyOperation(CaptionKey.Backspace, value(text = "world", caret = 0), hasPreviousCaption = true),
        )
    }

    // endregion
    // region Value changes (the route soft keyboards take)

    @Test
    fun `a newline committed in the middle splits at the caret`() {
        assertEquals(
            CaptionKeyOperation.SplitAt(5),
            operationFor(old = value("Hello world", caret = 5), new = value("Hello\n world", caret = 6)),
        )
    }

    @Test
    fun `a newline committed at the end adds a caption after`() {
        assertEquals(
            CaptionKeyOperation.AddCaptionAfter,
            operationFor(old = value("Hello", caret = 5), new = value("Hello\n", caret = 6)),
        )
    }

    @Test
    fun `a newline committed at the very start is consumed and ignored`() {
        assertEquals(
            CaptionKeyOperation.Ignore,
            operationFor(old = value("Hello", caret = 0), new = value("\nHello", caret = 1)),
        )
    }

    @Test
    fun `a newline replacing a selection splits at the start of that selection`() {
        assertEquals(
            CaptionKeyOperation.SplitAt(2),
            operationFor(
                old = selection("Hello world", selectionStart = 2, selectionEnd = 7),
                new = value("He\norld", caret = 3),
            ),
        )
    }

    @Test
    fun `a paste containing newlines passes through`() {
        assertEquals(
            CaptionKeyOperation.PassThrough,
            operationFor(old = value("Hello", caret = 5), new = value("Hello\nworld\n!", caret = 13)),
        )
    }

    @Test
    fun `a newline that closes the composition it was typed into is still a return`() {
        // The trace Gboard actually produces: the word just typed is still composing, and the Return both
        // confirms it and commits the newline. Rejecting this on the previous value's composition made
        // Return insert a line break instead of adding a caption.
        assertEquals(
            CaptionKeyOperation.AddCaptionAfter,
            operationFor(
                old = value("Hi", caret = 2, isComposing = true),
                new = value("Hi\n", caret = 3),
            ),
        )
    }

    @Test
    fun `a newline typed before a word that stays composing still splits`() {
        // The other trace Gboard produces: a Return mid-text leaves the word after the caret composing, so
        // the new value still carries a composition even though the keystroke is finished.
        assertEquals(
            CaptionKeyOperation.SplitAt(3),
            operationFor(
                old = selection("Hi there", selectionStart = 3, selectionEnd = 3, isComposing = true),
                new = selection("Hi \nthere", selectionStart = 4, selectionEnd = 4, isComposing = true),
            ),
        )
    }

    @Test
    fun `a composition still being assembled passes through`() {
        assertEquals(
            CaptionKeyOperation.PassThrough,
            operationFor(old = value("", caret = 0), new = value("に", caret = 1, isComposing = true)),
        )
    }

    @Test
    fun `a word autocorrected by the return that commits it keeps the correction`() {
        // Gboard confirms the composing word and commits the newline in one change, so the text the caption
        // ends up with must come from the new value — rebuilding it from the old one would restore the typo.
        val change = CaptionKeyboard.valueChangeOperation(
            old = value("helo", caret = 4, isComposing = true),
            new = value("hello\n", caret = 6),
        )
        assertEquals(CaptionKeyOperation.AddCaptionAfter, change.operation)
        assertEquals("hello", change.text)
    }

    @Test
    fun `the text handed to a split has the typed newline removed`() {
        val change = CaptionKeyboard.valueChangeOperation(
            old = value("Hi there", caret = 3),
            new = value("Hi \nthere", caret = 4),
        )
        assertEquals(CaptionKeyOperation.SplitAt(3), change.operation)
        assertEquals("Hi there", change.text)
    }

    @Test
    fun `a return typed into a caption that already holds a newline is still recognised`() {
        // Pasted line breaks are kept, so the count rule has to be relative rather than absolute.
        assertEquals(
            CaptionKeyOperation.SplitAt(4),
            operationFor(old = value("one\ntwo", caret = 4), new = value("one\n\ntwo", caret = 5)),
        )
    }

    @Test
    fun `ordinary typing passes through`() {
        assertEquals(
            CaptionKeyOperation.PassThrough,
            operationFor(old = value("Hell", caret = 4), new = value("Hello", caret = 5)),
        )
    }

    @Test
    fun `a deletion passes through`() {
        assertEquals(
            CaptionKeyOperation.PassThrough,
            operationFor(old = value("Hello", caret = 5), new = value("Hell", caret = 4)),
        )
    }

    @Test
    fun `a caret move that changes nothing else passes through`() {
        assertEquals(
            CaptionKeyOperation.PassThrough,
            operationFor(old = value("Hello", caret = 0), new = value("Hello", caret = 0)),
        )
    }

    @Test
    fun `an emoji typed at the caret passes through`() {
        // Two UTF-16 units, so the length delta alone would not tell it apart from a newline.
        assertEquals(
            CaptionKeyOperation.PassThrough,
            operationFor(old = value("Hi", caret = 2), new = value("Hi😀", caret = 4)),
        )
    }

    // endregion

    private fun operationFor(
        old: CaptionFieldValue,
        new: CaptionFieldValue,
    ) = CaptionKeyboard.valueChangeOperation(old = old, new = new).operation

    @Test
    fun `a return replacing a selection that spans a newline still splits`() {
        // Only reachable on a caption holding a pasted line break, but the newline count alone would read as ordinary typing.
        val change = CaptionKeyboard.valueChangeOperation(
            old = selection("a\nb", selectionStart = 1, selectionEnd = 2),
            new = value("a\nb", caret = 2),
        )
        assertEquals(CaptionKeyOperation.SplitAt(1), change.operation)
        assertEquals("ab", change.text)
    }

    @Test
    fun `a return replacing the whole of a multi-line caption adds one after`() {
        val change = CaptionKeyboard.valueChangeOperation(
            old = selection("a\nb", selectionStart = 0, selectionEnd = 3),
            new = value("\n", caret = 1),
        )
        assertEquals(CaptionKeyOperation.AddCaptionAfter, change.operation)
        assertEquals("", change.text)
    }

    @Test
    fun `a multi-line paste over a selection is still left alone`() {
        val change = CaptionKeyboard.valueChangeOperation(
            old = selection("a\nb", selectionStart = 1, selectionEnd = 2),
            new = value("a\nx\nb", caret = 4),
        )
        assertEquals(CaptionKeyOperation.PassThrough, change.operation)
    }

    private fun value(
        text: String,
        caret: Int = text.length,
        isComposing: Boolean = false,
    ) = CaptionFieldValue(text = text, selectionStart = caret, selectionEnd = caret, isComposing = isComposing)

    private fun selection(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
        isComposing: Boolean = false,
    ) = CaptionFieldValue(
        text = text,
        selectionStart = selectionStart,
        selectionEnd = selectionEnd,
        isComposing = isComposing,
    )
}
