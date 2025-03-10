package ly.img.editor.core

import androidx.compose.runtime.compositionLocalOf

/**
 * The scope of the editor. All the callbacks (both regular and composable) that configure the editor are within this scope object.
 */
abstract class EditorScope {
    protected abstract val impl: EditorContext

    // It is an extension function on purpose to make accessing this object more obvious that it's part of the EditorScope and
    // not a customer's property (italic in Android Studio makes it more obvious).

    /**
     * The context of the editor. This property should be used to access all the properties and functions within the editor.
     */
    val EditorScope.editorContext: EditorContext
        get() = impl
}

/**
 * Composition local containing currently active [EditorScope].
 */
val LocalEditorScope = compositionLocalOf<EditorScope> { error("No initial value!") }
