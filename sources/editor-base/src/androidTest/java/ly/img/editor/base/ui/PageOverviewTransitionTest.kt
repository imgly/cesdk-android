package ly.img.editor.base.ui

import android.view.View
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import ly.img.editor.base.engine.QueuedDispatcher
import ly.img.engine.Engine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class PageOverviewTransitionTest {
    @Test
    fun replacingQueuedRequestsOnlyAppliesTheLatestMode() = onMain {
        withTransition { transition, dispatcher, errors ->
            val applied = mutableListOf<String>()
            transition.start(true, null) { applied.add("first") }
            transition.start(false, null) { applied.add("second") }
            transition.start(true, null) { applied.add("latest") }
            dispatcher.runAll()
            assertEquals(listOf("latest"), applied)
            assertEquals(emptyList<Throwable>(), errors)
            assertCleared(transition)
        }
    }

    @Test
    fun cancellingAQueuedRequestPreventsTheModeCallback() = onMain {
        withTransition { transition, dispatcher, _ ->
            var calls = 0
            transition.start(true, null) { calls++ }
            transition.cancel()
            assertCleared(transition)
            dispatcher.runAll()
            assertEquals(0, calls)
            assertCleared(transition)
        }
    }

    @Test
    fun throwingModeCallbackIsReportedOnceAndClearsTheOverlay() = onMain {
        withTransition { transition, dispatcher, errors ->
            var calls = 0
            transition.start(true, null) {
                calls++
                error("Navigation failed")
            }
            dispatcher.runAll()
            assertEquals(1, calls)
            assertEquals(1, errors.size)
            assertCleared(transition)
        }
    }

    @Test
    fun snapshotSetupFailureStillAppliesTheModeAndReportsErrors() = onMain {
        val view = object : View(InstrumentationRegistry.getInstrumentation().targetContext) {
            override fun isAttachedToWindow() = true
        }
        withTransition(view) { transition, dispatcher, errors ->
            var calls = 0
            // The engine is stopped: getCurrentPage fails before a snapshot can be captured.
            transition.start(true, null) { calls++ }
            dispatcher.runAll()
            assertEquals(1, calls)
            assertEquals(1, errors.size)
            assertCleared(transition)
        }
    }

    private fun assertCleared(transition: PageOverviewTransition) {
        assertFalse(transition.running)
        assertNull(transition.image)
        assertNull(transition.page)
        assertNull(transition.heldPagesState)
    }

    private fun withTransition(
        view: View? = null,
        block: (PageOverviewTransition, QueuedDispatcher, List<Throwable>) -> Unit,
    ) {
        val dispatcher = QueuedDispatcher()
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val errors = mutableListOf<Throwable>()
        val transition = PageOverviewTransition(Engine.getInstance("page-transition-test"), scope, errors::add) { view }
        try {
            block(transition, dispatcher, errors)
        } finally {
            transition.cancel()
            scope.cancel()
            dispatcher.runAll()
        }
    }

    private fun onMain(block: suspend CoroutineScope.() -> Unit) = runBlocking(Dispatchers.Main.immediate, block)
}
