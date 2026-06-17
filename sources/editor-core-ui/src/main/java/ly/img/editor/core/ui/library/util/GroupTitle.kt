package ly.img.editor.core.ui.library.util

import android.annotation.SuppressLint
import android.content.Context

/**
 * Resolves the title of a section expanded from an asset [group] by looking up the string
 * resource named "<[groupTitleKeyPrefix]><[group]>" (check
 * [ly.img.editor.core.library.LibraryContent.Section.groupTitleKeyPrefix] for the key
 * convention). Falls back to the raw [group] id when no such resource exists, so customer-defined
 * groups without a translation still render (the source author's id verbatim).
 */
@SuppressLint("DiscouragedApi")
internal fun resolveGroupTitle(
    context: Context,
    groupTitleKeyPrefix: String,
    group: String,
): String {
    val resId = context.resources.getIdentifier(groupTitleKeyPrefix + group, "string", context.packageName)
    return if (resId != 0) context.getString(resId) else group
}
