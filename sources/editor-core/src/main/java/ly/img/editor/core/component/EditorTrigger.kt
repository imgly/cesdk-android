package ly.img.editor.core.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.Flow
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@Stable
class EditorTrigger internal constructor(
    private val state: MutableState<Int> = mutableStateOf(0),
) : ReadOnlyProperty<Any?, Any> {
    val key: Any
        get() = state.value

    override fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ): Any = state.value

    operator fun invoke() {
        state.value++
    }

    companion object
}

/**
 * Creates and remembers a new [EditorTrigger].
 *
 * Every time any of the [keys] are changed, the trigger will be triggered and [EditorTrigger.key]
 * will have a different value.
 *
 * ```kotlin
 * val activeSceneTrigger by EditorTrigger.remember {
 *     engine.scene.onActiveChanged()
 * }
 * val historyTrigger by EditorTrigger.remember(activeSceneTrigger) {
 *     engine.editor.onHistoryUpdated()
 * }
 * val otherTrigger = EditorTrigger.remember()
 * LaunchedEffect(historyTrigger) {
 *     // Will be triggered every time history or active scene changes.
 *     otherTrigger() // this will trigger otherTrigger
 * }
 * LaunchedEffect(otherTrigger.key) {
 *     // ...
 * }
 * ```
 *
 * @param keys the keys that trigger the trigger whenever they change.
 * @return a new trigger instance.
 */
@Composable
fun EditorTrigger.Companion.remember(vararg keys: Any?): EditorTrigger {
    val trigger = androidx.compose.runtime.remember { EditorTrigger() }
    androidx.compose.runtime.remember(*keys) {
        trigger()
        true
    }
    return trigger
}

/**
 * Creates and remembers a new [EditorTrigger].
 *
 * Every time any of the [keys] are changed, the trigger will be triggered and [EditorTrigger.key]
 * will have a different value.
 * Every time the flow returned by [flow] lambda collects a new value, the trigger will be triggered and
 * [EditorTrigger.key] will have a different value.
 *
 * ```kotlin
 * val activeSceneTrigger by EditorTrigger.remember {
 *     engine.scene.onActiveChanged()
 * }
 * val historyTrigger by EditorTrigger.remember(activeSceneTrigger) {
 *     engine.editor.onHistoryUpdated()
 * }
 * val otherTrigger = EditorTrigger.remember()
 * LaunchedEffect(historyTrigger) {
 *     // Will be triggered every time history or active scene changes.
 *     otherTrigger() // this will trigger otherTrigger
 * }
 * LaunchedEffect(otherTrigger.key) {
 *     // ...
 * }
 * ```
 *
 * @param keys the keys that trigger the trigger whenever they change.
 * @param flow lambda that provides a flow, whose collection triggers the trigger.
 * @return a new trigger instance.
 */
@Composable
fun EditorTrigger.Companion.remember(
    vararg keys: Any?,
    flow: () -> Flow<*>,
): EditorTrigger {
    val trigger = remember(*keys)
    LaunchedEffect(*keys) {
        flow().collect { trigger() }
    }
    return trigger
}
