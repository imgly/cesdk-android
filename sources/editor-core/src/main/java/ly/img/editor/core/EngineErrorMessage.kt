package ly.img.editor.core

import android.content.Context
import ly.img.engine.EngineException

/**
 * Resolves this structured engine error to the customer-facing message to display.
 *
 * Looks up the authored `ly_img_engine_error_<code>` string resource for this exception's
 * [code][EngineException.code], interpolating any `{{name}}` placeholders with the matching
 * [args][EngineException.args], and falling back to the engine's English [message][Throwable.message]
 * when no copy is authored — so a surface never shows a blank or a raw code.
 *
 * Cast a thrown error with `as? EngineException` first; for other throwables keep your own fallback,
 * e.g. `(error as? EngineException)?.getDisplayMessage(context) ?: error.message`.
 *
 * The catalog id `SCENE.NOT_VALID` maps to `ly_img_engine_error_scene_not_valid`.
 */
fun EngineException.getDisplayMessage(context: Context): String {
    val fallback = message ?: ""
    if (code.isEmpty()) {
        return fallback
    }
    return resolveErrorString(context, errorResourceName(), args) ?: fallback
}

/**
 * Resolves this structured engine error to the customer-facing body copy to display below
 * [getDisplayMessage], or `null` when there is nothing to show.
 *
 * The longer-form companion to [getDisplayMessage], mirroring the `description` half of the Web
 * resolver and the engine's own message/hint split. Looks up the authored
 * `ly_img_engine_error_<code>_description` string resource, interpolating any `{{name}}`
 * placeholders with the matching [args][EngineException.args], and falling back to the engine's
 * English [hint][EngineException.hint] when no copy is authored. Returns `null` when no copy is
 * authored and the catalog declares no hint, so a surface can omit the line instead of showing a
 * blank.
 *
 * The catalog id `SCENE.NOT_VALID` maps to `ly_img_engine_error_scene_not_valid_description`.
 */
fun EngineException.getDisplayDescription(context: Context): String? {
    val fallback = hint.ifEmpty { null }
    if (code.isEmpty()) {
        return fallback
    }
    return resolveErrorString(context, errorResourceName() + "_description", args) ?: fallback
}

/** `SCENE.NOT_VALID` → `ly_img_engine_error_scene_not_valid`. */
private fun EngineException.errorResourceName(): String = "ly_img_engine_error_" + code.lowercase().replace('.', '_')

/**
 * Looks up [resourceName] as a string resource and interpolates [args], or returns `null` when the
 * resource is absent or empty — so callers fall back to the engine string.
 */
private fun resolveErrorString(
    context: Context,
    resourceName: String,
    args: Map<String, Any>,
): String? {
    val resId = context.resources.getIdentifier(resourceName, "string", context.packageName)
    if (resId == 0) {
        return null
    }
    val localized = context.getString(resId)
    return if (localized.isNotEmpty()) localized.interpolateArgs(args) else null
}

/**
 * Substitutes `{{name}}` placeholders in an authored override with the matching entry from the
 * engine error's [args][EngineException.args] (`value.toString()`).
 * A placeholder with no matching arg is left intact, as the engine's own template rendering does.
 * The engine `message`/`hint` fallback is already interpolated and never passes through here.
 *
 * This is intentionally the `{{name}}` convention, not Android's native `%1$s`/`<plurals>` formatting:
 * the resource is read with the no-arg [Context.getString], so `String.format` args and quantity
 * strings never apply, and an override written the `%1$s` way would render a literal `%1$s`.
 */
private fun String.interpolateArgs(args: Map<String, Any>): String {
    if (args.isEmpty() || !contains("{{")) return this
    var result = this
    for ((name, value) in args) {
        result = result.replace("{{$name}}", value.toString())
    }
    return result
}
