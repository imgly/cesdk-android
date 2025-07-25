package ly.img.editor.core.library

import ly.img.editor.core.R
import ly.img.editor.core.library.LibraryCategory.Companion.sourceTypes
import ly.img.editor.core.library.LibraryContent.Section
import ly.img.editor.core.library.data.AssetSourceType
import ly.img.engine.SceneMode

/**
 * Configuration class for the asset library.
 *
 * @param tabs a provider for the list of categories that are displayed as tabs in the asset library.
 * @param elements a provider for the category that is displaying elements tab.
 * @param images a provider for the category that is displayed when inserting/replacing an image asset.
 * @param videos a provider for the category that is displayed when inserting/replacing a video asset.
 * @param audios a provider for the category that is displayed when inserting/replacing an audio asset.
 * @param text a provider for the category that is displayed when inserting a text asset.
 * @param shapes a provider for the category that is displayed when inserting/replacing a shape asset.
 * @param stickers a provider for the category that is displayed when inserting/replacing a sticker asset.
 * @param overlays a provider for the category that is displayed when inserting an overlay.
 * @param clips a provider for the category that is displayed when inserting a clip.
 * @param stickersAndShapes a provider for the category that is displayed when inserting stickers and shapes.
 */
data class AssetLibrary(
    val tabs: (SceneMode) -> List<LibraryCategory>,
    val elements: (SceneMode) -> LibraryCategory = { LibraryCategory.getElements(it) },
    val images: (SceneMode) -> LibraryCategory = { LibraryCategory.Images },
    val videos: (SceneMode) -> LibraryCategory = { LibraryCategory.Video },
    val audios: (SceneMode) -> LibraryCategory = { LibraryCategory.Audio },
    val text: (SceneMode) -> LibraryCategory = { LibraryCategory.Text },
    val shapes: (SceneMode) -> LibraryCategory = { LibraryCategory.Shapes },
    val stickers: (SceneMode) -> LibraryCategory = { LibraryCategory.Stickers },
    val overlays: (SceneMode) -> LibraryCategory = { createOverlaysCategory(videos(it), images(it)) },
    val clips: (SceneMode) -> LibraryCategory = { createClipsCategory(videos(it), images(it)) },
    val stickersAndShapes: (SceneMode) -> LibraryCategory = {
        createStickersAndShapesCategory(
            stickers = stickers(it),
            shapes = shapes(it),
        )
    },
) {
    /**
     * Predefined tabs that can be displayed in the asset library.
     */
    enum class Tab {
        ELEMENTS,
        IMAGES,
        VIDEOS,
        AUDIOS,
        TEXT,
        SHAPES,
        STICKERS,
    }

    companion object {
        /**
         * A helper function for creating an [AssetLibrary] instance. This is an ideal builder in case you just want
         * to swap positions of tabs or drop some of the tabs. You can also modify a specific category by completely replacing it
         * or modifying using the helper functions [LibraryCategory.addSection], [LibraryCategory.dropSection] and
         * [LibraryCategory.replaceSection].
         *
         * @param tabs the list of tabs that should be displayed. The tabs are displayed in the same order as that of this list.
         * @param images the images category that is used in the tabs and the [images].
         * @param videos the videos category that is used in the tabs and the [videos].
         * @param audios the audios category that is used in the tabs and the [audios].
         * @param text the text category that is used in the tabs and the [text].
         * @param textAndTextComponents the text category along with text components.
         * @param shapes the shapes category that is used in the tabs and the [shapes].
         * @param stickers the stickers category that is used in the tabs and the [stickers].
         * @param overlays the overlays category.
         * @param clips the clips category.
         * @param stickersAndShapes the stickers and shapes category.
         */
        fun getDefault(
            tabs: List<Tab> = Tab.entries,
            images: LibraryCategory = LibraryCategory.Images,
            videos: LibraryCategory = LibraryCategory.Video,
            audios: LibraryCategory = LibraryCategory.Audio,
            text: LibraryCategory = LibraryCategory.Text,
            textAndTextComponents: LibraryCategory = createTextWithTextComponentsCategory(text = text),
            shapes: LibraryCategory = LibraryCategory.Shapes,
            stickers: LibraryCategory = LibraryCategory.Stickers,
            overlays: LibraryCategory = createOverlaysCategory(videos = videos, images = images),
            clips: LibraryCategory = createClipsCategory(videos = videos, images = images),
            stickersAndShapes: LibraryCategory = createStickersAndShapesCategory(stickers = stickers, shapes = shapes),
        ): AssetLibrary {
            fun getElements(sceneMode: SceneMode): LibraryCategory = LibraryCategory.getElements(
                sceneMode = sceneMode,
                images = images,
                videos = videos,
                audios = audios,
                text = text,
                textAndTextComponents = textAndTextComponents,
                shapes = shapes,
                stickers = stickers,
            )
            return AssetLibrary(
                tabs = { sceneMode ->
                    tabs.mapNotNull {
                        when (it) {
                            Tab.ELEMENTS -> getElements(sceneMode)
                            Tab.IMAGES -> images
                            Tab.VIDEOS -> if (sceneMode == SceneMode.VIDEO) videos else null
                            Tab.AUDIOS -> if (sceneMode == SceneMode.VIDEO) audios else null
                            Tab.TEXT -> if (sceneMode == SceneMode.VIDEO) text else textAndTextComponents
                            Tab.SHAPES -> shapes
                            Tab.STICKERS -> stickers
                        }
                    }
                },
                elements = ::getElements,
                images = { images },
                videos = { videos },
                audios = { audios },
                text = { if (it == SceneMode.VIDEO) text else textAndTextComponents },
                shapes = { shapes },
                stickers = { stickers },
                overlays = { overlays },
                clips = { clips },
                stickersAndShapes = { stickersAndShapes },
            )
        }

        private fun createTextWithTextComponentsCategory(text: LibraryCategory): LibraryCategory {
            require(text.content is LibraryContent.Sections)
            return text.copy(
                content = text.content.copy(
                    sections =
                        text.content.sections.map {
                            it.copy(titleRes = it.titleRes ?: R.string.ly_img_editor_asset_library_section_plain_text)
                        } + Section(
                            titleRes = R.string.ly_img_editor_asset_library_section_font_combinations,
                            sourceTypes = listOf(AssetSourceType.TextComponents),
                            assetType = AssetType.TextComponent,
                        ),
                ),
            )
        }

        private fun createOverlaysCategory(
            videos: LibraryCategory,
            images: LibraryCategory,
        ): LibraryCategory = LibraryCategory.Overlays
            .replaceSection(0) {
                copy(
                    sourceTypes = videos.content.sourceTypes,
                    expandContent = videos.content,
                )
            }.replaceSection(1) {
                copy(
                    sourceTypes = images.content.sourceTypes,
                    expandContent = images.content,
                )
            }

        private fun createClipsCategory(
            videos: LibraryCategory,
            images: LibraryCategory,
        ): LibraryCategory = LibraryCategory.Clips
            .replaceSection(0) {
                copy(
                    sourceTypes = videos.content.sourceTypes,
                    expandContent = videos.content,
                )
            }.replaceSection(1) {
                copy(
                    sourceTypes = images.content.sourceTypes,
                    expandContent = images.content,
                )
            }

        private fun createStickersAndShapesCategory(
            stickers: LibraryCategory,
            shapes: LibraryCategory,
        ): LibraryCategory {
            require(stickers.content is LibraryContent.Sections)
            require(shapes.content is LibraryContent.Sections)
            return LibraryCategory.StickersAndShapes.copy(
                content = LibraryContent.StickersAndShapes.copy(
                    sections = stickers.content.sections + shapes.content.sections,
                ),
            )
        }
    }
}
