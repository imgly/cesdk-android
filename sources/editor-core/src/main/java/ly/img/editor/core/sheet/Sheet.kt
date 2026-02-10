package ly.img.editor.core.sheet

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ly.img.editor.core.EditorScope
import ly.img.editor.core.R
import ly.img.editor.core.UnstableEditorApi
import ly.img.editor.core.component.data.Height
import ly.img.editor.core.library.LibraryCategory
import ly.img.editor.core.sheet.SheetType.Custom

/**
 * An interface representing different types of sheets used in the editor.
 * Below you can find some of the implementations of this interface.
 * Avoid inheriting from this interface. If you want to have a custom sheet consider using [Custom] instead.
 */
interface SheetType {
    /**
     * the style that should be used to display the sheet.
     */
    val style: SheetStyle

    /**
     * A custom sheet with custom [content].
     * Note that if you need the content to be scrollable, then you need to add it manually.
     *
     * @param style the style that should be used to display the sheet.
     * @param content the content that should be rendered in the sheet.
     */
    class Custom(
        override val style: SheetStyle,
        val content: @Composable EditorScope.() -> Unit,
    ) : SheetType

    /**
     * A sheet that should be used to display [LibraryCategory] in order to add assets in the scene.
     *
     * @param style the style that should be used to display the sheet.
     * Default value is [SheetStyle] that is floating, fullscreen and is initially half-expanded.
     * @param libraryCategory the library category that should be displayed in the sheet.
     * Default value is null, which means the sheet will display the tabs returned by [ly.img.editor.core.library.AssetLibrary.tabs].
     * @param addToBackgroundTrack whether assets picked from this sheet should be placed on the background track when working on
     * a video scene. Default value is false.
     */
    class LibraryAdd(
        override val style: SheetStyle = SheetStyle(
            isFloating = true,
            maxHeight = Height.Fraction(1F),
            isHalfExpandingEnabled = true,
        ),
        val libraryCategory: LibraryCategory? = null,
        val addToBackgroundTrack: Boolean = false,
    ) : SheetType

    /**
     * A sheet that should be used to display [LibraryCategory] in order to replace assets in the scene.
     *
     * @param style the style that should be used to display the sheet.
     * Default value is [SheetStyle] that is not floating, fullscreen and is initially half-expanded.
     * @param libraryCategory the library category that should be displayed in the sheet.
     */
    class LibraryReplace(
        override val style: SheetStyle = SheetStyle(
            isFloating = false,
            maxHeight = Height.Fraction(1F),
            isHalfExpandingEnabled = true,
            isHalfExpandedInitially = true,
        ),
        val libraryCategory: LibraryCategory,
    ) : SheetType

    /**
     * A sheet that is used to reorder videos on the background track.
     *
     * @param style the style that should be used to display the sheet.
     * Default value is the default [SheetStyle].
     */
    class Reorder(
        override val style: SheetStyle = SheetStyle(),
    ) : SheetType

    /**
     * A sheet that is used to make adjustments to design blocks with image and video fills.
     *
     * @param style the style that should be used to display the sheet.
     * Default value is the default [SheetStyle].
     */
    class Adjustments(
        override val style: SheetStyle = SheetStyle(),
    ) : SheetType

    /**
     * A sheet that is used to set filters to design blocks with image and video fills.
     *
     * @param style the style that should be used to display the sheet.
     * Default value is the default [SheetStyle].
     */
    class Filter(
        override val style: SheetStyle = SheetStyle(),
    ) : SheetType

    /**
     * A sheet that is used to set effects to design blocks with image and video fills.
     *
     * @param style the style that should be used to display the sheet.
     * Default value is the default [SheetStyle] with the minimum height set exactly to 204.dp.
     */
    class Effect(
        // The minHeight 204.dp is chosen to match the height of the "Main" screen, while you are on the "Adjustment" screen.
        override val style: SheetStyle = SheetStyle(
            minHeight = Height.Exactly(204.dp),
        ),
    ) : SheetType

    /**
     * A sheet that is used to set blurs to design blocks with image and video fills.
     *
     * @param style the style that should be used to display the sheet.
     * Default value is the default [SheetStyle].
     */
    class Blur(
        override val style: SheetStyle = SheetStyle(),
    ) : SheetType

    /**
     * A sheet that is used to crop design blocks with image and video fills.
     *
     * @param style the style that should be used to display the sheet.
     * Default value is the default [SheetStyle].
     * @param mode controls which crop options are available.
     */
    open class Crop(
        override val style: SheetStyle = SheetStyle(),
        val mode: Mode,
    ) : SheetType {
        /**
         * Configuration of the different crop and resize modes.
         *
         * Each flag enables or disables certain controls of the [Crop] sheet.
         *
         * @property titleRes String resource used as the title of the sheet.
         * @property hasCropAsset whether the sheet should show crop assets.
         * @property hasPageAsset whether page assets can be selected.
         * @property hasResetButton whether a reset button is displayed.
         * @property hasResizeOption whether resize controls are available.
         * @property applyOnAllPages if `true`, changes apply to all pages.
         * @property hasRotateOptions whether rotation controls are visible.
         * @property hasContentFillMode whether content fill mode can be changed.
         */
        interface Mode {
            val titleRes: Int
            val hasCropAsset: Boolean
            val hasPageAsset: Boolean
            val hasResetButton: Boolean
            var hasResizeOption: Boolean
            val applyOnAllPages: Boolean
            val hasRotateOptions: Boolean
            val hasContentFillMode: Boolean

            /** Default mode for cropping image or video fills. */
            data object ImageCrop : Mode {
                override val titleRes = R.string.ly_img_editor_sheet_crop_title

                override val hasCropAsset: Boolean = true
                override val hasPageAsset: Boolean = true

                override val hasResetButton: Boolean = true
                override var hasResizeOption: Boolean = true
                override val applyOnAllPages: Boolean = false
                override val hasRotateOptions: Boolean = true
                override val hasContentFillMode: Boolean = true
            }

            /** Used when cropping the current page image fill. */
            data object PageCrop : Mode {
                override val titleRes = R.string.ly_img_editor_sheet_crop_title

                override val hasCropAsset: Boolean = false
                override val hasPageAsset: Boolean = true

                override val hasResetButton: Boolean = true
                override var hasResizeOption: Boolean = true
                override val applyOnAllPages: Boolean = false
                override val hasRotateOptions: Boolean = true
                override val hasContentFillMode: Boolean = true
            }

            /** Crops the currently selected design element. */
            data object Element : Mode {
                override val titleRes = R.string.ly_img_editor_sheet_crop_title

                override val hasCropAsset: Boolean = true
                override val hasPageAsset: Boolean = false

                override val hasResetButton: Boolean = true
                override var hasResizeOption: Boolean = false
                override val applyOnAllPages: Boolean = false
                override val hasRotateOptions: Boolean = true
                override val hasContentFillMode: Boolean = true
            }

            /** Resizes all pages of the design. */
            data object ResizeAll : Mode {
                override val titleRes = R.string.ly_img_editor_sheet_resize_title

                override val hasCropAsset: Boolean = false
                override val hasPageAsset: Boolean = true

                override val hasResetButton: Boolean = false
                override var hasResizeOption: Boolean = true
                override val applyOnAllPages: Boolean = true
                override val hasRotateOptions: Boolean = false
                override val hasContentFillMode: Boolean = false
            }
        }
    }

    /**
     * A sheet that is used to crop design blocks with image and video fills.
     *
     * @param style the style that should be used to display the sheet.
     * Default value is the default [SheetStyle].
     */
    class ResizeAll(
        override val style: SheetStyle = SheetStyle(),
    ) : Crop(
            style = style,
            mode = Mode.ResizeAll,
        )

    /**
     * A sheet that is used to control the layering of design blocks.
     *
     * @param style the style that should be used to display the sheet.
     * Default value is the default [SheetStyle].
     */
    class Layer(
        override val style: SheetStyle = SheetStyle(),
    ) : SheetType

    /**
     * A sheet that is used to control formatting of text blocks.
     *
     * @param style the style that should be used to display the sheet.
     * Default value is the default [SheetStyle].
     */
    class FormatText(
        override val style: SheetStyle = SheetStyle(),
    ) : SheetType

    /**
     * A sheet that is used to control the shape of various blocks.
     *
     * @param style the style that should be used to display the sheet.
     * Default value is the default [SheetStyle].
     */
    class Shape(
        override val style: SheetStyle = SheetStyle(),
    ) : SheetType

    /**
     * A sheet that is used to control the fill and/or stroke of various blocks.
     *
     * @param style the style that should be used to display the sheet.
     * Default value is the default [SheetStyle].
     */
    class FillStroke(
        override val style: SheetStyle = SheetStyle(),
    ) : SheetType

    /**
     * A sheet that is used to control the playback speed of video clips.
     *
     * @param style the style that should be used to display the sheet.
     * Default value is the default [SheetStyle].
     */
    class Speed(
        override val style: SheetStyle = SheetStyle(),
    ) : SheetType

    /**
     * A sheet that is used to control the volume of audio/video.
     *
     * @param style the style that should be used to display the sheet.
     * Default value is the default [SheetStyle].
     */
    class Volume(
        override val style: SheetStyle = SheetStyle(),
    ) : SheetType

    /**
     * A sheet that is used to set in/out/loop animation to design blocks.
     *
     * @param style the style that should be used to display the sheet.
     * Default value is the default [SheetStyle].
     */
    class Animation(
        override val style: SheetStyle = SheetStyle(),
    ) : SheetType

    /**
     * A sheet that is used to control the background color and shape of text block.
     *
     * @param style the style that should be used to display the sheet.
     * Default value is the default [SheetStyle].
     */
    class TextBackground(
        override val style: SheetStyle = SheetStyle(),
    ) : SheetType

    /**
     * A sheet that is used to control greeting font in [ly.img.editor.PostcardEditor].
     *
     * @param style the style that should be used to display the sheet.
     * Default value is the default [SheetStyle].
     */
    @UnstableEditorApi
    class PostcardGreetingFont(
        override val style: SheetStyle = SheetStyle(),
    ) : SheetType

    /**
     * A sheet that is used to control greeting font size in [ly.img.editor.PostcardEditor].
     *
     * @param style the style that should be used to display the sheet.
     * Default value is the default [SheetStyle].
     */
    @UnstableEditorApi
    class PostcardGreetingSize(
        override val style: SheetStyle = SheetStyle(),
    ) : SheetType

    /**
     * A sheet that is used to control greeting the colors [ly.img.editor.PostcardEditor].
     *
     * @param style the style that should be used to display the sheet.
     * Default value is the default [SheetStyle].
     * @param colorMapping the mappings between design block name and current color that should be displayed.
     */
    @UnstableEditorApi
    class PostcardColors(
        override val style: SheetStyle = SheetStyle(),
        val colorMapping: Map<String, Color>,
    ) : SheetType
}

/**
 * A class representing the style of sheets.
 *
 * @param isFloating whether the sheet should be floating. If true the sheet will be rendered over the editor, if false then the canvas
 * will be zoomed to adjust the sheet.
 * Default value is false.
 * @param minHeight the minimum height of the sheet.
 * Default value is 0.
 * @param maxHeight the maximum height of the sheet. If null, then there is no limit to the height. Once the maximum
 * height is reached, the content of the sheet becomes vertically scrollable, if it is supported.
 * Note that if you need the content to be scrollable in [SheetType.Custom], then you need to add it manually.
 * Default value is half the height of the editor.
 * @param isHalfExpandingEnabled whether the sheet should have a half expanded state.
 * If false, then the sheet gets only 2 states: hidden and expanded.
 * Default value is true.
 * @param isHalfExpandedInitially whether the sheet should be opened as half expanded initially.
 * This flag takes effect only if [isHalfExpandingEnabled] is true.
 * Default value is false.
 * @param animateInitialValue whether initial expanded or half-expanded state should be animated.
 */
data class SheetStyle(
    val isFloating: Boolean = false,
    val minHeight: Height = Height.Exactly(0.dp),
    val maxHeight: Height? = Height.Fraction(0.5F),
    val isHalfExpandingEnabled: Boolean = false,
    val isHalfExpandedInitially: Boolean = false,
    val animateInitialValue: Boolean = true,
)
