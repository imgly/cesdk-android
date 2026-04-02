package ly.img.editor.core

/**
 * The current language code derived from the Activity's configuration (e.g., `"en"`, `"de"`).
 */
val EditorContext.currentLanguageCode: String
    get() {
        val locales = activity.resources.configuration.locales
        return if (!locales.isEmpty) locales[0].language else java.util.Locale.getDefault().language
    }
