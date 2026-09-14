package ly.img.editor.base.engine

import androidx.compose.ui.graphics.Color
import ly.img.editor.core.component.data.Insets
import ly.img.editor.core.ui.engine.Scope
import ly.img.editor.core.ui.engine.dpToCanvasUnit
import ly.img.editor.core.ui.engine.getCamera
import ly.img.editor.core.ui.engine.getPage
import ly.img.editor.core.ui.engine.getStackOrNull
import ly.img.editor.core.ui.engine.overrideAndRestore
import ly.img.engine.BlockApi
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine
import ly.img.engine.PositionMode

fun Engine.setClearColor(color: Color) {
    editor.setSettingColor("clearColor", color.toEngineColor())
}

fun Engine.resetHistory() {
    val oldHistory = editor.getActiveHistory()
    val newHistory = editor.createHistory()
    editor.setActiveHistory(newHistory)
    editor.destroyHistory(oldHistory)
    editor.addUndoStep()
}

fun Engine.isPlaceholder(designBlock: DesignBlock): Boolean {
    if (!block.supportsPlaceholderControls(designBlock)) return false
    val isPlaceholderEnabled = block.isPlaceholderEnabled(designBlock)
    val showsPlaceholderButton = block.isPlaceholderControlsButtonEnabled(designBlock)
    val showsPlaceholderOverlay = block.isPlaceholderControlsOverlayEnabled(designBlock)
    return isPlaceholderEnabled && (showsPlaceholderButton || showsPlaceholderOverlay)
}

fun Engine.canBringForward(designBlock: DesignBlock): Boolean {
    val parent = block.getParent(designBlock)
    parent ?: return false
    val children = getReorderableChildren(parent, designBlock)
    return children.last() != designBlock
}

fun Engine.canSendBackward(designBlock: DesignBlock): Boolean {
    val parent = block.getParent(designBlock)
    parent ?: return false
    val children = getReorderableChildren(parent, designBlock)
    return children.first() != designBlock
}

/**
 * Get all reorderable children for a given parent and contained child.
 * This method filters out children that are not reorderable with the given child.
 */
private fun Engine.getReorderableChildren(
    parent: DesignBlock,
    child: DesignBlock,
): List<DesignBlock> {
    val childIsAlwaysOnTop = block.isAlwaysOnTop(child)
    val childIsAlwaysOnBottom = block.isAlwaysOnBottom(child)
    val childType = block.getType(child)

    val children = block.getChildren(parent)

    return children.filter { childToCompare ->
        val matchingIsAlwaysOnTop = childIsAlwaysOnTop == block.isAlwaysOnTop(childToCompare)
        val matchingIsAlwaysOnBottom = childIsAlwaysOnBottom == block.isAlwaysOnBottom(childToCompare)
        val matchingType = when (childType) {
            DesignBlockType.Audio.key -> block.getType(childToCompare) == DesignBlockType.Audio.key
            else -> block.getType(childToCompare) != DesignBlockType.Audio.key
        }
        matchingIsAlwaysOnTop && matchingIsAlwaysOnBottom && matchingType
    }
}

fun Engine.duplicate(designBlock: DesignBlock) {
    val duplicateBlock = block.duplicate(designBlock)
    if (!block.isTransformLocked(designBlock)) {
        val positionModeX = block.getPositionXMode(designBlock)
        val positionModeY = block.getPositionYMode(designBlock)
        overrideAndRestore(designBlock, Scope.LayerMove) {
            block.setPositionXMode(it, PositionMode.ABSOLUTE)
            val x = block.getPositionX(it)
            block.setPositionYMode(it, PositionMode.ABSOLUTE)
            val y = block.getPositionY(it)
            block.setPositionXMode(duplicateBlock, PositionMode.ABSOLUTE)
            block.setPositionX(duplicateBlock, x + 5)
            block.setPositionYMode(duplicateBlock, PositionMode.ABSOLUTE)
            block.setPositionY(duplicateBlock, y - 5)

            // Restore values
            block.setPositionXMode(it, positionModeX)
            block.setPositionYMode(it, positionModeY)
        }
    }
    block.setSelected(designBlock, false)
    block.setSelected(duplicateBlock, true)
    editor.addUndoStep()
}

fun Engine.delete(designBlock: DesignBlock) {
    block.destroy(designBlock)
    editor.addUndoStep()
}

/**
 * An extension function for checking whether the [designBlock] can be moved up/down.
 * IMPORTANT! When modifying this function also modify similar function in InspectorBarExt.kt.
 *
 * @return true if the [designBlock] can be moved, false otherwise.
 */
fun Engine.isMoveAllowed(designBlock: DesignBlock): Boolean = block.isAllowedByScope(designBlock, Scope.LayerMove) &&
    !block.isParentBackgroundTrack(designBlock)

/**
 * An extension function for checking whether the [designBlock] can be duplicated.
 * IMPORTANT! When modifying this function also modify similar function in InspectorBarExt.kt.
 *
 * @return true if the [designBlock] can be duplicated, false otherwise.
 */
fun Engine.isDuplicateAllowed(designBlock: DesignBlock): Boolean = block.isAllowedByScope(designBlock, Scope.LifecycleDuplicate)

/**
 * An extension function for checking whether the [designBlock] can be deleted.
 * IMPORTANT! When modifying this function also modify similar function in InspectorBarExt.kt.
 *
 * @return true if the [designBlock] can be deleted, false otherwise.
 */
fun Engine.isDeleteAllowed(designBlock: DesignBlock): Boolean = block.isAllowedByScope(designBlock, Scope.LifecycleDestroy)

fun Engine.zoomToPage(
    pageIndex: Int,
    insets: Insets,
) {
    scene.immediateZoomToBlock(
        block = getPage(pageIndex),
        paddingLeft = insets.left.value,
        paddingTop = insets.top.value,
        paddingRight = insets.right.value,
        paddingBottom = insets.bottom.value,
        forceUpdate = true,
    )
}

fun Engine.zoomToSelectedText(
    insets: Insets,
    canvasHeight: Float,
) {
    val paddingTop = insets.top.value
    val paddingBottom = insets.bottom.value

    val overlapTop = 50
    val overlapBottom = 50

    block.findAllSelected().singleOrNull() ?: return
    val pixelRatio = block.getFloat(getCamera(), "camera/pixelRatio")
    val cursorPosY = editor.getTextCursorPositionInScreenSpaceY() / pixelRatio
    // The first cursorPosY is 0 if no cursor has been layout yet. Then we ignore zoom commands.
    if (cursorPosY == 0f) return
    val visiblePageAreaY = canvasHeight - overlapBottom - paddingBottom
    val visiblePageAreaYCanvas = dpToCanvasUnit(visiblePageAreaY)
    val cursorPosYCanvas = dpToCanvasUnit(cursorPosY)
    val cameraPosY = block.getPositionY(getCamera())
    val newCameraPosY = cursorPosYCanvas + cameraPosY - visiblePageAreaYCanvas

    if (cursorPosY > visiblePageAreaY || cursorPosY < (overlapTop + paddingTop)) {
        overrideAndRestore(getCamera(), Scope.LayerMove) {
            block.setPositionY(getCamera(), newCameraPosY)
        }
    }
}

fun Engine.showPage(index: Int) {
    getStackOrNull()?.let { stack ->
        block.setEnum(stack, "stack/axis", LayoutAxis.Depth.name)
    }
    scene.getPages().forEachIndexed { idx, page ->
        overrideAndRestore(page, Scope.LayerVisibility) {
            block.setVisible(block = it, visible = idx == index)
        }
    }
}

/**
 * Returns the block that exposes playback control for the given [designBlock].
 */
fun BlockApi.getPlaybackControlBlock(designBlock: DesignBlock): DesignBlock? = when {
    supportsPlaybackControl(designBlock) -> designBlock
    supportsFill(designBlock) -> {
        val fill = getFill(designBlock)
        if (supportsPlaybackControl(fill)) {
            fill
        } else {
            null
        }
    }
    else -> null
}
