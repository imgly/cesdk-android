package ly.img.editor.version.details.entity

internal data class Release(
    val key: String,
    val createTime: String,
    val downloadUrl: String,
    val branchName: String,
    val commitId: String,
    val isCurrentBuild: Boolean,
) {
    companion object {
        val UpToDate = Release(
            key = "",
            createTime = "",
            downloadUrl = "",
            branchName = "",
            commitId = "",
            isCurrentBuild = false,
        )
    }
}
