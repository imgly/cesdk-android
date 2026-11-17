package ly.img.editor.base.timeline.state

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.base.ui.handler.animationEvents
import ly.img.editor.core.ui.EventsHandler
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine
import ly.img.engine.TransitionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class TransitionDropTest {
    @Test
    fun applyToAllCopiesOverlapRegardlessOfTheCreationDefault() = withEngine { engine ->
        val (track, outgoing, middle) = createClips(engine)
        val incoming = engine.block.create(DesignBlockType.Graphic)
        engine.block.setDuration(incoming, 5.0)
        engine.block.appendChild(track, incoming)
        val original = engine.block.createTransition(TransitionType.CrossFade)
        engine.block.setTransition(outgoing, original)

        coroutineScope {
            val events = EventsHandler(this) {
                animationEvents(engine = { engine }, timelineState = { null })
            }
            for (overlap in listOf(false, true)) {
                engine.block.setBoolean(original, "transition/overlap", overlap)
                engine.editor.setSettingBoolean("clip/defaultTransitionOverlapEnabled", !overlap)
                events.handleEvent(BlockEvent.OnApplyTransitionToTrack(outgoing)).join()
                assertEquals(overlap, engine.block.getBoolean(engine.block.getTransition(middle), "transition/overlap"))
            }
        }
    }

    @Test
    fun mixedTransitionsUseTheirOwnOverlapAfterDefaultChanges() = withEngine { engine ->
        engine.editor.setSettingBoolean("clip/defaultTransitionOverlapEnabled", false)
        val (track, outgoing, middle) = createClips(engine)
        val incoming = engine.block.create(DesignBlockType.Graphic)
        engine.block.setDuration(incoming, 5.0)
        engine.block.appendChild(track, incoming)
        val fixedTransition = engine.block.createTransition(TransitionType.CrossFade)
        engine.block.setDuration(fixedTransition, 1.0)
        engine.block.setTransition(outgoing, fixedTransition)
        val overlappingTransition = engine.block.createTransition(TransitionType.CrossFade)
        engine.block.setBoolean(overlappingTransition, "transition/overlap", true)
        engine.block.setDuration(overlappingTransition, 1.0)
        engine.block.setTransition(middle, overlappingTransition)

        engine.editor.setSettingBoolean("clip/defaultTransitionOverlapEnabled", true)

        assertEquals(14.0, engine.block.getDuration(track), 0.001)
        assertEquals(0.seconds, engine.transitionOverlap(outgoing, middle))
        assertEquals(1.seconds, engine.transitionOverlap(middle, incoming))
        assertEquals(0.seconds, engine.transitionOverlapForDrop(outgoing, middle))
        assertEquals(1.seconds, engine.transitionOverlapForDrop(middle, incoming))
        assertEquals(0.seconds, engine.transitionTrim(outgoing).tail)
        assertEquals(0.seconds, engine.transitionTrim(middle).lead)
        assertEquals(0.5.seconds, engine.transitionTrim(middle).tail)
        assertEquals(0.5.seconds, engine.transitionTrim(incoming).lead)
    }

    @Test
    fun aGroupCutNeverOverlapsWhateverItsTransitionSaves() = withEngine { engine ->
        val (track, outgoing, _) = createClips(engine)
        val graphic = engine.block.create(DesignBlockType.Graphic)
        engine.block.appendChild(requireNotNull(engine.block.getParent(track)), graphic)
        val group = engine.block.group(listOf(graphic))
        engine.block.setDuration(graphic, 5.0)
        engine.block.setDuration(group, 5.0)
        engine.block.insertChild(track, group, 1)
        val transition = engine.block.createTransition(TransitionType.CrossFade)
        engine.block.setBoolean(transition, "transition/overlap", true)
        engine.block.setDuration(transition, 1.0)
        engine.block.setTransition(outgoing, transition)

        assertEquals(0.seconds, engine.transitionOverlap(outgoing, group))
        assertEquals(0.seconds, engine.transitionOverlapForDrop(outgoing, group))
        assertEquals(0.seconds, engine.transitionTrim(outgoing).tail)
        assertEquals(0.seconds, engine.transitionTrim(group).lead)
    }

    @Test
    fun foregroundStickerDropUsesTheNewTransitionPartner() = withEngine { engine ->
        val (track, outgoing, incoming) = createClips(engine)
        val transition = engine.block.createTransition(TransitionType.CrossFade)
        engine.block.setDuration(transition, 1.0)
        engine.block.setTransition(outgoing, transition)
        assertEquals(9.0, engine.block.getDuration(track), 0.001)
        engine.block.setBoolean(track, "track/automaticallyManageBlockOffsets", false)
        val sticker = engine.block.create(DesignBlockType.Graphic)
        engine.block.setKind(sticker, "sticker")
        engine.block.setDuration(sticker, 2.0)
        engine.block.setTimeOffset(sticker, 20.0)
        engine.block.insertChild(track, sticker, 1)
        val clips = listOf(
            DropClip(outgoing, 0.seconds, 5.seconds, 0.seconds, 4.5.seconds),
            DropClip(sticker, 20.seconds, 2.seconds, 4.5.seconds, 6.5.seconds),
            DropClip(incoming, 4.seconds, 5.seconds, 6.5.seconds, 11.seconds),
        )

        val offsets = computeDropOffsets(clips, sticker, setOf(sticker, incoming), engine::transitionOverlapForDrop)

        assertEquals(4.seconds, offsets.getValue(sticker))
        assertEquals(6.seconds, offsets.getValue(incoming))
        offsets.forEach { (clip, offset) -> engine.block.setTimeOffset(clip, offset.inWholeMilliseconds / 1000.0) }
        assertEquals(11.0, engine.block.getDuration(track), 0.001)
        assertEquals(transition, engine.block.getTransition(outgoing))
        assertEquals(sticker, engine.transitionIncomingClip(outgoing))
    }

    @Test
    fun insertingStickerPreservesTransitionWithFixedClipTiming() = withEngine { engine ->
        engine.editor.setSettingBoolean("clip/defaultTransitionOverlapEnabled", false)
        val (track, outgoing, incoming) = createClips(engine)
        val transition = engine.block.createTransition(TransitionType.CrossFade)
        engine.block.setDuration(transition, 1.0)
        engine.block.setTransition(outgoing, transition)
        val sticker = engine.block.create(DesignBlockType.Graphic)
        engine.block.setKind(sticker, "sticker")
        engine.block.setDuration(sticker, 2.0)

        engine.block.insertChild(track, sticker, 1)

        assertTrue("Inserting a new neighbor must preserve the outgoing transition", engine.block.isValid(transition))
        assertEquals(transition, engine.block.getTransition(outgoing))
        // Reading the track duration lays out the inserted clip before checking its timeline position.
        assertEquals(12.0, engine.block.getDuration(track), 0.001)
        assertEquals(sticker, engine.transitionIncomingClip(outgoing))
        assertEquals(5.0, engine.block.getDuration(outgoing), 0.001)
        assertEquals(5.0, engine.block.getDuration(incoming), 0.001)
        assertEquals(5.0, engine.block.getTimeOffset(sticker), 0.001)
        assertEquals(7.0, engine.block.getTimeOffset(incoming), 0.001)
    }

    @Test
    fun insertingStickerPreservesTransitionWithOverlapEnabled() = withEngine { engine ->
        engine.editor.setSettingBoolean("clip/defaultTransitionOverlapEnabled", true)
        val (track, outgoing, _) = createClips(engine)
        val transition = engine.block.createTransition(TransitionType.CrossFade)
        engine.block.setDuration(transition, 1.0)
        engine.block.setTransition(outgoing, transition)
        val sticker = engine.block.create(DesignBlockType.Graphic)
        engine.block.setKind(sticker, "sticker")
        engine.block.setDuration(sticker, 2.0)

        engine.block.insertChild(track, sticker, 1)

        assertTrue("Inserting a new neighbor must preserve the outgoing transition", engine.block.isValid(transition))
        assertEquals(transition, engine.block.getTransition(outgoing))
        assertEquals(11.0, engine.block.getDuration(track), 0.001)
        assertEquals(sticker, engine.transitionIncomingClip(outgoing))
        assertEquals(4.0, engine.block.getTimeOffset(sticker), 0.001)
    }

    private fun createClips(engine: Engine): Triple<Int, Int, Int> {
        val scene = engine.scene.create()
        engine.block.setEnum(scene, "scene/mode", "Video")
        val page = engine.block.create(DesignBlockType.Page)
        engine.block.appendChild(scene, page)
        val track = engine.block.create(DesignBlockType.Track)
        engine.block.setBoolean(track, "track/automaticallyManageBlockOffsets", true)
        engine.block.appendChild(page, track)
        val clips = List(2) {
            engine.block.create(DesignBlockType.Graphic).also { clip ->
                engine.block.setDuration(clip, 5.0)
                engine.block.appendChild(track, clip)
            }
        }
        return Triple(track, clips[0], clips[1])
    }

    private fun withEngine(test: suspend (Engine) -> Unit) = runBlocking(Dispatchers.Main) {
        val application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        Engine.init(application)
        val engine = Engine.getInstance("transition-drop-test")
        engine.start()
        try {
            test(engine)
        } finally {
            engine.stop()
        }
    }
}
