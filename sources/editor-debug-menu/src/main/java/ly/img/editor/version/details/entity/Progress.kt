package ly.img.editor.version.details.entity

internal sealed interface Progress {
    data object Installing : Progress

    data class Pending(
        val progress: Float,
    ) : Progress

    data class Error(
        val throwable: Throwable,
    ) : Progress
}
