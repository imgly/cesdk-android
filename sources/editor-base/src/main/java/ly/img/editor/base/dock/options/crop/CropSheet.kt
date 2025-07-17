package ly.img.editor.base.dock.options.crop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.flow.MutableStateFlow
import ly.img.editor.base.R
import ly.img.editor.base.dock.BottomSheetContent
import ly.img.editor.base.dock.CustomBottomSheetContent
import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.compose.bottomsheet.ModalBottomSheetState
import ly.img.editor.compose.bottomsheet.ModalBottomSheetValue
import ly.img.editor.compose.bottomsheet.SwipeableDefaults
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.iconpack.Resize
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.core.ui.IconTextRowButton
import ly.img.editor.core.ui.SheetHeader
import ly.img.editor.core.ui.UiDefaults
import ly.img.editor.core.ui.iconpack.Flip
import ly.img.editor.core.ui.iconpack.FreeCrop
import ly.img.editor.core.ui.iconpack.IconPack
import ly.img.editor.core.ui.iconpack.Reset
import ly.img.editor.core.ui.iconpack.Rotate90degreesccwoutline
import ly.img.editor.core.ui.iconpack.SquareCrop
import ly.img.editor.core.ui.library.CropLibraryCategory
import ly.img.editor.core.ui.library.CustomAssetList
import ly.img.editor.core.ui.library.ItemContentPayload
import ly.img.editor.core.ui.library.SelectableAssetList
import ly.img.editor.core.ui.sheet.Sheet
import ly.img.engine.AssetTransformPreset
import ly.img.engine.ContentFillMode
import ly.img.engine.DesignUnit
import kotlin.math.roundToInt
import ly.img.editor.core.iconpack.IconPack as CoreIconPack

open class CropBottomSheetContent(
    override val type: SheetType,
    val uiState: CropUiState,
) : BottomSheetContent

@Composable
fun CropSheet(
    uiState: CropUiState,
    onEvent: (EditorEvent) -> Unit,
) {
    val assetListState = rememberLazyListState()
    val groupListState = rememberLazyListState()

    var selectedGroup by remember {
        mutableStateOf<CropGroup?>(uiState.groups.first())
    }

    var isResizeDialogOpen by remember { mutableStateOf(false) }

    SheetHeader(
        title = stringResource(id = uiState.cropMode.titleRes),
        onClose = { onEvent(EditorEvent.Sheet.Close(animate = true)) },
        actionContent = if (uiState.cropMode.hasResetButton) {
            {
                IconTextRowButton(
                    modifier = Modifier.padding(start = 8.dp, end = 4.dp),
                    icon = {
                        Icon(IconPack.Reset, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    text = {
                        Text(stringResource(R.string.ly_img_editor_reset))
                    },
                    enabled = uiState.canResetCrop,
                    onClick = {
                        onEvent(BlockEvent.OnResetCrop)
                    },
                )
            }
        } else {
            null
        },
    )
    Column(
        Modifier
            .wrapContentHeight(),
    ) {
        if (uiState.cropMode.hasRotateOptions) {
            Card(
                colors = UiDefaults.cardColors,
                modifier = Modifier
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp),
            ) {
                Row {
                    IconButton(
                        enabled = true,
                        modifier = Modifier.align(Alignment.Bottom),
                        onClick = { onEvent(BlockEvent.OnFlipCropHorizontal) },
                    ) {
                        Icon(IconPack.Flip, contentDescription = stringResource(R.string.ly_img_editor_flip))
                    }
                    ScalePicker(
                        valuePrefix = stringResource(R.string.ly_img_editor_straighten),
                        value = uiState.straightenAngle,
                        valueRange = -45f..45f,
                        // Use +-44.999 as bound to guarantee that `decomposedDegrees` is stable and thus
                        // `straightenDegrees` won't jump from -45 to +45 or vice versa for some 90 degree rotations.
                        rangeInclusionType = RangeInclusionType.RangeExclusiveExclusive,
                        onValueChange = {
                            onEvent(BlockEvent.OnCropStraighten(it, uiState.cropScaleRatio))
                        },
                        onValueChangeFinished = {
                            if (it != uiState.straightenAngle) {
                                onEvent(BlockEvent.OnChangeFinish)
                            }
                        },
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 16.dp)
                            .weight(1f),
                    )
                    IconButton(
                        enabled = true,
                        modifier = Modifier.align(Alignment.Bottom),
                        onClick = { onEvent(BlockEvent.OnCropRotate(uiState.cropScaleRatio)) },
                    ) {
                        Icon(IconPack.Rotate90degreesccwoutline, contentDescription = stringResource(R.string.ly_img_editor_rotate))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (selectedGroup?.sourceId == uiState.pageAssetSourceId) {
            SelectableAssetList(
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                selection = uiState.selectedAssetKey,
                selectionKey = { "" },
                libraryCategory = CropLibraryCategory.Page,
                listState = assetListState,
                onAssetSelected = {
                    onEvent(
                        BlockEvent.OnReplaceCropPreset(
                            wrappedAsset = it,
                            applyOnAllPages = uiState.cropMode.applyOnAllPages,
                        ),
                    )
                },
                groupFilter = selectedGroup?.id,
                addSeparator = true,
                hasNoneItem = false,
            )
        } else if (selectedGroup?.sourceId == uiState.cropAssetSourceId) {
            CustomAssetList(
                modifier = Modifier.height(64.dp + 16.dp).padding(top = 8.dp, bottom = 8.dp),
                selection = uiState.selectedAssetKey,
                selectionKey = { "" },
                libraryCategory = CropLibraryCategory.Crop,
                listState = assetListState,
                onAssetSelected = {
                    onEvent(
                        BlockEvent.OnReplaceCropPreset(
                            wrappedAsset = it,
                            applyOnAllPages = uiState.cropMode.applyOnAllPages,
                        ),
                    )
                },
                contentPadding = PaddingValues(horizontal = 0.dp),
                groupFilter = selectedGroup?.id,
                addSeparator = true,
                hasNoneItem = false,
            ) {
                AspectRatioItem()
            }
        }
    }
    if (uiState.cropMode.hasResizeOption || uiState.cropMode.hasContentFillMode || uiState.groups.size > 1) {
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp),
        )

        Column(
            Modifier
                .wrapContentHeight()
                .navigationBarsPadding(),
        ) {
            LazyRow(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 16.dp)
                    .height(40.dp)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                state = groupListState,
            ) {
                if (uiState.cropMode.hasContentFillMode) {
                    item {
                        ContentFillModePicker(uiState = uiState, onEvent = onEvent)
                    }
                }
                if (uiState.cropMode.hasResizeOption) {
                    item {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    isResizeDialogOpen = true
                                }
                                .background(
                                    color = if (isResizeDialogOpen) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                    shape = RoundedCornerShape(20.dp),
                                )
                                .padding(start = 12.dp, end = 8.dp),
                        ) {
                            Icon(
                                imageVector = CoreIconPack.Resize,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(18.dp)
                                    .align(Alignment.CenterVertically),
                            )
                            Text(
                                text =
                                    "${uiState.resizeState.width.roundToInt()} × ${uiState.resizeState.height.roundToInt()} ${uiState.resizeState.unit.unitSuffix}",
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
                if (uiState.groups.size > 1) {
                    if (uiState.cropMode.hasResizeOption || uiState.cropMode.hasContentFillMode) {
                        item {
                            Divider(
                                modifier = Modifier
                                    .height(24.dp)
                                    .width(1.dp)
                                    .align(Alignment.CenterHorizontally),
                            )
                        }
                    }

                    items(uiState.groups) { group ->
                        val isSelected = group == selectedGroup
                        Text(
                            text = group.name,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { selectedGroup = group }
                                .background(
                                    color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                    shape = RoundedCornerShape(20.dp),
                                )
                                .padding(horizontal = 24.dp, vertical = 10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
    if (isResizeDialogOpen) {
        Dialog(onDismissRequest = { isResizeDialogOpen = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
            ) {
                ResizeDialog(
                    uiState = uiState.resizeState,
                    applyOnAllPages = uiState.cropMode.applyOnAllPages,
                    onEvent = onEvent,
                    onClose = { isResizeDialogOpen = false },
                )
            }
        }
    }
}

@Composable
fun ItemContentPayload.AspectRatioItem() {
    val wrappedAsset = wrappedAsset
    if (wrappedAsset != null) {
        Column(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable {
                    if (isSelected) {
                        onAssetReselected(wrappedAsset)
                    } else {
                        onAssetSelected(wrappedAsset)
                    }
                }
                .padding(top = 6.dp, bottom = 4.dp, start = 4.dp, end = 4.dp),
        ) {
            val transformPreset = wrappedAsset.asset.payload.transformPreset
            Icon(
                when (transformPreset) {
                    is AssetTransformPreset.FixedAspectRatio -> {
                        if (transformPreset.width == transformPreset.height) {
                            IconPack.SquareCrop
                        } else {
                            roundedRectOutlineIcon(
                                transformPreset.width / transformPreset.height,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    is AssetTransformPreset.FixedSize -> {
                        if (transformPreset.width == transformPreset.height) {
                            IconPack.SquareCrop
                        } else {
                            roundedRectOutlineIcon(
                                transformPreset.width / transformPreset.height,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    else -> {
                        // Fallback to a square icon if no aspect ratio is defined
                        IconPack.FreeCrop
                    }
                },
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp, 24.dp)
                    .align(Alignment.CenterHorizontally),
            )
            Text(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(64.dp)
                    .padding(top = 7.dp),
                text = wrappedAsset.asset.label ?: "",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

// ////////////////////////////////////////////////////////////////////////////
// /////////////////////          PREVIEW         /////////////////////////////
// ////////////////////////////////////////////////////////////////////////////

@Composable()
fun SheetPreview(mode: SheetType.Crop.Mode = SheetType.Crop.Mode.ImageCrop) {
    val style: SheetType.Crop = SheetType.Crop(mode = mode)
    val scrimBottomSheetState = remember(mode) {
        ModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Hidden,
            animationSpec = SwipeableDefaults.AnimationSpec,
            confirmValueChange = { true },
            isSkipHalfExpanded = false,
        )
    }
    LaunchedEffect(mode) {
        scrimBottomSheetState.snapTo(ModalBottomSheetValue.HalfExpanded)
    }
    val bottomSheetContentState by MutableStateFlow<BottomSheetContent?>(
        CustomBottomSheetContent(
            type = style,
            content = {},
        ),
    ).collectAsState()

    val bottomSheetContent = bottomSheetContentState!!
    val sheetStyle by remember(bottomSheetContent.type) { mutableStateOf(bottomSheetContent.type.style) }
    Column {
        Sheet(style = sheetStyle) {
            Column {
                CropSheet(
                    uiState = CropUiState(
                        cropMode = style.mode,
                        resizeState = ResizeUiState(
                            width = 800f,
                            height = 600f,
                            dpi = 1,
                            pixelScaleFactor = 1f,
                            unit = DesignUnitEntry(
                                titleRes = R.string.ly_img_editor_unit_inch,
                                native = DesignUnit.INCH,
                                values = listOf(72f, 150f, 300f, 600f, 1200f, 2400f),
                                defaultValue = 300f,
                                valueSuffix = " dpi",
                                unitValueNameRes = R.string.ly_img_editor_unit_dpi_value_name,
                                unitSuffix = "inch",
                            ),
                        ),
                        contentFillMode = ContentFillMode.CROP,
                        straightenAngle = 0f,
                        canResetCrop = true,
                        cropScaleRatio = 1f,
                        pageAssetSourceId = "page_source",
                        cropAssetSourceId = "crop_source",
                        selectedAssetKey = "",
                        groups = listOf(
                            CropGroup("group1", "Group 1"),
                            CropGroup("group2", "Group 2"),
                        ),
                    ),
                    onEvent = {},
                )
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun ImageCropPreview() = SheetPreview(SheetType.Crop.Mode.ImageCrop)

@Composable
@Preview(showBackground = true)
fun ElementPreview() = SheetPreview(SheetType.Crop.Mode.Element)

@Composable
@Preview(showBackground = true)
fun PageCropPreview() = SheetPreview(SheetType.Crop.Mode.PageCrop)

@Composable
@Preview(showBackground = true)
fun ResizeAllPreview() = SheetPreview(SheetType.Crop.Mode.ResizeAll)
