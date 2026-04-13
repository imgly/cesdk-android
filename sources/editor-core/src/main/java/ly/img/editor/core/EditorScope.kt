package ly.img.editor.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf

/**
 * Scope of the editor. All the callbacks (both regular and composable) that configure the editor are within this scope object.
 */
@Stable
open class EditorScope internal constructor(
    private val editorContextImpl: EditorContext,
) {
    /**
     * @param parentScope the parent scope that should own this child scope.
     */
    constructor(parentScope: EditorScope) : this(parentScope.editorContextImpl)

    /**
     * The context of the editor. This property should be used to access all the properties and functions within the editor.
     * It is an extension function on purpose to make accessing this object more obvious that it's part of the EditorScope and
     * not a customer's property (italic in Android Studio makes it more obvious).
     */
    val EditorScope.editorContext: EditorContext
        get() = editorContextImpl
}

/**
 * Composition local containing currently active [EditorScope]. Avoid using this composition local at all cost.
 */
val LocalEditorScope = compositionLocalOf {
    EditorScope(editorContextImpl = EditorContextImpl())
}

/**
 * Represents composable lambda that runs in [Scope] which value can be updated as a result of recompositions.
 */
typealias ScopedProperty<Scope, Return> = @Composable Scope.() -> Return

/**
 * Represents composable lambda that runs in [Scope] and should be used to decorate other composables.
 */
typealias ScopedDecoration<Scope> = @Composable Scope.(@Composable () -> Unit) -> Unit
