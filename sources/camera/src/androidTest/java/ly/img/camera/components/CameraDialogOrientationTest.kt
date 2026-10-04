package ly.img.camera.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import ly.img.camera.record.components.DeleteAllRecordingsDialog
import ly.img.camera.record.components.DeleteLastRecordingDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CameraDialogOrientationTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun deleteLastDialogRotatesWhileOpenAndConfirmRemainsClickable() {
        checkDialog(dialog = Dialog.DeleteLast, confirm = true)
    }

    @Test
    fun deleteAllDialogRotatesWhileOpenAndCancelRemainsClickable() {
        checkDialog(dialog = Dialog.DeleteAll, confirm = false)
    }

    @Test
    fun videoErrorDialogRotatesWhileOpenAndCloseRemainsClickable() {
        checkDialog(dialog = Dialog.VideoError, confirm = true)
    }

    private fun checkDialog(
        dialog: Dialog,
        confirm: Boolean,
    ) {
        val orientation = mutableStateOf(0)
        var confirms = 0
        var dismissals = 0
        compose.setContent {
            MaterialTheme {
                when (dialog) {
                    Dialog.DeleteAll -> DeleteAllRecordingsDialog(
                        orientation = orientation.value,
                        rotation = { orientation.value.toFloat() },
                        onConfirm = { confirms++ },
                        onDismiss = { dismissals++ },
                    )
                    Dialog.DeleteLast -> DeleteLastRecordingDialog(
                        orientation = orientation.value,
                        rotation = { orientation.value.toFloat() },
                        onConfirm = { confirms++ },
                        onDismiss = { dismissals++ },
                    )

                    Dialog.VideoError -> VideoErrorDialog(
                        orientation = orientation.value,
                        rotation = { orientation.value.toFloat() },
                        onClose = { confirms++ },
                    )
                }
            }
        }

        val title = when (dialog) {
            Dialog.DeleteAll -> "Close and delete all recordings?"
            Dialog.DeleteLast -> "Delete last recording?"
            Dialog.VideoError -> "Failed to load video"
        }
        val confirmLabel = when (dialog) {
            Dialog.DeleteAll -> "Delete all"
            Dialog.DeleteLast -> "Delete"
            Dialog.VideoError -> "Close"
        }
        val actionLabel = if (confirm) confirmLabel else "Cancel"
        for (angle in listOf(0, 90, 180, 270)) {
            compose.runOnIdle { orientation.value = angle }
            compose.waitForIdle()
            compose.onNodeWithText(title).assertIsDisplayed()
            compose.onNodeWithText(confirmLabel).assertIsDisplayed()
            if (dialog == Dialog.VideoError) {
                compose.onNodeWithText("Cancel").assertDoesNotExist()
            } else {
                compose.onNodeWithText("Cancel").assertIsDisplayed()
            }
            val titleBounds = compose.onNodeWithText(title).fetchSemanticsNode().boundsInRoot
            val buttonBounds = compose.onNodeWithText(actionLabel).fetchSemanticsNode().boundsInRoot
            // The title stays visually above the actions in each device orientation.
            assertTrue(
                "Dialog content did not rotate to $angle degrees: title=$titleBounds, button=$buttonBounds",
                when (angle) {
                    90 -> titleBounds.left > buttonBounds.right
                    180 -> titleBounds.top > buttonBounds.bottom
                    270 -> titleBounds.right < buttonBounds.left
                    else -> titleBounds.bottom < buttonBounds.top
                },
            )
            compose.onNodeWithText(actionLabel).performTouchInput { click() }
        }

        compose.runOnIdle {
            assertEquals(if (confirm) 4 else 0, confirms)
            assertEquals(if (confirm) 0 else 4, dismissals)
        }
    }

    private enum class Dialog {
        DeleteLast,
        DeleteAll,
        VideoError,
    }
}
