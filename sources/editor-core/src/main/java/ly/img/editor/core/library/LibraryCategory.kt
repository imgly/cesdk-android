package ly.img.editor.core.library

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import ly.img.editor.core.R
import ly.img.editor.core.iconpack.IconPack
import ly.img.editor.core.iconpack.Image
import ly.img.editor.core.iconpack.ImageOutline
import ly.img.editor.core.iconpack.LibraryElements
import ly.img.editor.core.iconpack.LibraryElementsOutline
import ly.img.editor.core.iconpack.Music
import ly.img.editor.core.iconpack.PlayBox
import ly.img.editor.core.iconpack.PlayBoxOutline
import ly.img.editor.core.iconpack.Shapes
import ly.img.editor.core.iconpack.ShapesOutline
import ly.img.editor.core.iconpack.StickerEmoji
import ly.img.editor.core.iconpack.StickerEmojiOutline
import ly.img.editor.core.iconpack.TextFields
import ly.img.editor.core.library.data.AssetSourceType
import ly.img.engine.SceneMode

/**
 * Configuration class of the UI of each library category. Each category contains a title (check [tabTitleRes]) and a [content]
 * to render the UI. If the category is part of the tabs specified in [AssetLibrary.tabs], [tabSelectedIcon] and
 * [tabUnselectedIcon] are used to display the icon of the category in the tabs.
 *
 * @param tabTitleRes string resource of the category's title that is displayed at the very top of the UI of [content].
 * @param tabSelectedIcon the icon to display when the category is displayed as a tab element and the tab is selected.
 * @param tabSelectedIcon the icon to display when the category is displayed as a tab element and the tab is not selected.
 * @param isHalfExpandedInitially whether bottom sheet should open half expanded or fully expanded first.
 * @param content the content of the category. Check the documentation of [LibraryContent] for more information.
 */
@Immutable
data class LibraryCategory(
    @StringRes val tabTitleRes: Int,
    val tabSelectedIcon: ImageVector,
    val tabUnselectedIcon: ImageVector,
    @Deprecated("Parameter is unused. Consider configuring it via SheetType.LibraryAdd.mode or SheetType.LibraryReplace.mode")
    val isHalfExpandedInitially: Boolean = false,
    val content: LibraryContent,
) {
    companion object {
        /**
         * A helper function to construct an abstract "Elements" category that is a combination of categories.
         */
        fun getElements(
            sceneMode: SceneMode,
            images: LibraryCategory = Images,
            videos: LibraryCategory = Video,
            audios: LibraryCategory = Audio,
            text: LibraryCategory = Text,
            shapes: LibraryCategory = Shapes,
            stickers: LibraryCategory = Stickers,
        ): LibraryCategory {
            val isSceneModeVideo = sceneMode == SceneMode.VIDEO
            return LibraryCategory(
                tabTitleRes = R.string.ly_img_editor_asset_library_title_elements,
                tabSelectedIcon = IconPack.LibraryElements,
                tabUnselectedIcon = IconPack.LibraryElementsOutline,
                content = LibraryContent.Sections(
                    titleRes = R.string.ly_img_editor_asset_library_title_elements,
                    sections = buildList {
                        val gallerySource = if (isSceneModeVideo) {
                            listOf(AssetSourceType.GalleryAllVisuals)
                        } else {
                            listOf(AssetSourceType.GalleryImage)
                        }
                        LibraryContent.Section(
                            titleRes = R.string.ly_img_editor_asset_library_section_gallery,
                            sourceTypes = gallerySource,
                            showUpload = false,
                            assetType = AssetType.Gallery,
                            expandContent = LibraryContent.Grid(
                                titleRes = R.string.ly_img_editor_asset_library_section_gallery,
                                sourceType = gallerySource.first(),
                                assetType = AssetType.Gallery,
                            ),
                        ).let(::add)

                        if (isSceneModeVideo) {
                            videos.apply {
                                LibraryContent.Section(
                                    titleRes = R.string.ly_img_editor_asset_library_section_videos,
                                    sourceTypes = content.sourceTypes,
                                    assetType = AssetType.Video,
                                    expandContent = content,
                                ).let(::add)
                            }
                            audios.apply {
                                LibraryContent.Section(
                                    titleRes = R.string.ly_img_editor_asset_library_section_audio,
                                    sourceTypes = content.sourceTypes,
                                    count = 3,
                                    assetType = AssetType.Audio,
                                    expandContent = content,
                                ).let(::add)
                            }
                        }

                        images.apply {
                            LibraryContent.Section(
                                titleRes = R.string.ly_img_editor_asset_library_section_images,
                                sourceTypes = content.sourceTypes,
                                assetType = AssetType.Image,
                                expandContent = content,
                            ).let(::add)
                        }

                        text.apply {
                            LibraryContent.Section(
                                titleRes = R.string.ly_img_editor_asset_library_section_text,
                                sourceTypes = content.sourceTypes,
                                excludedPreviewSourceTypes = listOf(AssetSourceType.TextComponents),
                                assetType = AssetType.Text,
                                expandContent = content,
                            ).let(::add)
                        }

                        shapes.apply {
                            LibraryContent.Section(
                                titleRes = R.string.ly_img_editor_asset_library_section_shapes,
                                sourceTypes = content.sourceTypes,
                                assetType = AssetType.Shape,
                                expandContent = content,
                            ).let(::add)
                        }

                        stickers.apply {
                            LibraryContent.Section(
                                titleRes = R.string.ly_img_editor_asset_library_title_stickers,
                                sourceTypes = content.sourceTypes,
                                assetType = AssetType.Sticker,
                                expandContent = content,
                            ).let(::add)
                        }
                    },
                ),
            )
        }

        fun getGallery(sceneMode: SceneMode): LibraryCategory {
            val isVideoScene = sceneMode == SceneMode.VIDEO
            val sections = if (isVideoScene) {
                listOf(
                    LibraryContent.Section(
                        titleRes = R.string.ly_img_editor_asset_library_section_gallery,
                        sourceTypes = listOf(AssetSourceType.GalleryAllVisuals),
                        assetType = AssetType.Gallery,
                        expandContent = LibraryContent.Grid(
                            titleRes = R.string.ly_img_editor_asset_library_section_gallery,
                            sourceType = AssetSourceType.GalleryAllVisuals,
                            assetType = AssetType.Gallery,
                        ),
                    ),
                )
            } else {
                listOf(
                    LibraryContent.Section(
                        titleRes = R.string.ly_img_editor_asset_library_section_gallery,
                        sourceTypes = listOf(AssetSourceType.GalleryImage),
                        assetType = AssetType.Image,
                    ),
                )
            }
            return LibraryCategory(
                tabTitleRes = R.string.ly_img_editor_asset_library_section_gallery,
                tabSelectedIcon = IconPack.Image,
                tabUnselectedIcon = IconPack.ImageOutline,
                content = LibraryContent.Sections(
                    titleRes = R.string.ly_img_editor_asset_library_section_gallery,
                    sections = sections,
                ),
            )
        }

        /**
         * The default library category for video assets.
         */
        private fun createVideoCategory(includeSystemGallery: Boolean): LibraryCategory = LibraryCategory(
            tabTitleRes = R.string.ly_img_editor_asset_library_title_videos,
            tabSelectedIcon = IconPack.PlayBox,
            tabUnselectedIcon = IconPack.PlayBoxOutline,
            content = LibraryContent.videos(includeSystemGallery = includeSystemGallery),
        )

        val Video: LibraryCategory
            get() = createVideoCategory(includeSystemGallery = true)

        /**
         * Variant of [Video] that omits the system gallery section (only curated sources and uploads).
         */
        val VideoWithoutSystemGallery: LibraryCategory
            get() = createVideoCategory(includeSystemGallery = false)

        /**
         * The default library category for audio assets.
         */
        val Audio by lazy {
            LibraryCategory(
                tabTitleRes = R.string.ly_img_editor_asset_library_title_audio,
                tabSelectedIcon = IconPack.Music,
                tabUnselectedIcon = IconPack.Music,
                content = LibraryContent.Audio,
            )
        }

        /**
         * The default library category for image assets.
         */
        private fun createImagesCategory(includeSystemGallery: Boolean): LibraryCategory = LibraryCategory(
            tabTitleRes = R.string.ly_img_editor_asset_library_title_images,
            tabSelectedIcon = IconPack.Image,
            tabUnselectedIcon = IconPack.ImageOutline,
            content = LibraryContent.images(includeSystemGallery = includeSystemGallery),
        )

        val Images: LibraryCategory
            get() = createImagesCategory(includeSystemGallery = true)

        /**
         * Variant of [Images] that omits the system gallery section (only curated sources and uploads).
         */
        val ImagesWithoutSystemGallery: LibraryCategory
            get() = createImagesCategory(includeSystemGallery = false)

        /**
         * The default library category for text assets.
         */
        val Text by lazy {
            LibraryCategory(
                tabTitleRes = R.string.ly_img_editor_asset_library_title_text,
                tabSelectedIcon = IconPack.TextFields,
                tabUnselectedIcon = IconPack.TextFields,
                isHalfExpandedInitially = true,
                content = LibraryContent.Text,
            )
        }

        /**
         * The default library category for shape assets.
         */
        val Shapes by lazy {
            LibraryCategory(
                tabTitleRes = R.string.ly_img_editor_asset_library_title_shapes,
                tabSelectedIcon = IconPack.Shapes,
                tabUnselectedIcon = IconPack.ShapesOutline,
                content = LibraryContent.Shapes,
            )
        }

        /**
         * The default library category for sticker assets.
         */
        val Stickers by lazy {
            LibraryCategory(
                tabTitleRes = R.string.ly_img_editor_asset_library_title_stickers,
                tabSelectedIcon = IconPack.StickerEmoji,
                tabUnselectedIcon = IconPack.StickerEmojiOutline,
                content = LibraryContent.Stickers,
            )
        }

        /**
         * The default library category for overlay assets.
         */
        val Overlays: LibraryCategory
            get() = LibraryCategory(
                tabTitleRes = R.string.ly_img_editor_asset_library_title_overlays,
                tabSelectedIcon = IconPack.PlayBox,
                tabUnselectedIcon = IconPack.PlayBoxOutline,
                content = LibraryContent.Overlays,
            )

        /**
         * The default library category for clip assets.
         */
        val Clips: LibraryCategory
            get() = LibraryCategory(
                tabTitleRes = R.string.ly_img_editor_asset_library_title_clips,
                tabSelectedIcon = IconPack.PlayBox,
                tabUnselectedIcon = IconPack.PlayBoxOutline,
                content = LibraryContent.Clips,
            )

        /**
         * The default library category for sticker and shape assets.
         */
        val StickersAndShapes: LibraryCategory
            get() = LibraryCategory(
                tabTitleRes = R.string.ly_img_editor_asset_library_title_stickers_and_shapes,
                tabSelectedIcon = IconPack.StickerEmoji,
                tabUnselectedIcon = IconPack.StickerEmojiOutline,
                content = LibraryContent.StickersAndShapes,
            )

        /**
         * All the source types of the library content.
         */
        val LibraryContent.sourceTypes: List<AssetSourceType>
            get() = when (this) {
                is LibraryContent.Sections ->
                    sections
                        .flatMap { it.sourceTypes }
                        .toSet()
                        .toList()
                is LibraryContent.Grid -> listOf(sourceType)
            }
    }
}

/**
 * Add a new section to the content of the library category. Note that the function will throw an exception if the
 * content of the library category is not [LibraryContent.Sections].
 *
 * @param section the section to add at the bottom.
 */
fun LibraryCategory.addSection(section: LibraryContent.Section): LibraryCategory {
    require(content is LibraryContent.Sections) {
        "addSection can be called only for categories that have sections content."
    }
    return copy(content = content.copy(sections = content.sections + section))
}

/**
 * Drop a section from the content of the library category. Note that the function will throw an exception if the
 * content of the library category is not [LibraryContent.Sections].
 *
 * @param index the index to drop.
 */
fun LibraryCategory.dropSection(index: Int): LibraryCategory {
    require(content is LibraryContent.Sections) {
        "addSection can be called only for categories that have sections content."
    }
    return content.sections.toMutableList().run {
        removeAt(index)
        copy(content = content.copy(sections = this))
    }
}

/**
 * Replace a section in the content of the library category. Note that the function will throw an exception if the
 * content of the library category is not [LibraryContent.Sections].
 *
 * @param index the index to replace.
 * @param sectionReducer the reducer that converts the existing section at [index] into a new one.
 */
fun LibraryCategory.replaceSection(
    index: Int,
    sectionReducer: LibraryContent.Section.() -> LibraryContent.Section,
): LibraryCategory {
    require(content is LibraryContent.Sections) {
        "replaceSection can be called only for categories that have sections content."
    }
    val sections = content.sections.mapIndexed { internalIndex, section ->
        if (index == internalIndex) sectionReducer(section) else section
    }
    return copy(content = content.copy(sections = sections))
}
