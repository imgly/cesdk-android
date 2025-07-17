package ly.img.editor.base.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ly.img.editor.compose.foundation.clickable

@Composable
fun <T : Any> Tabs(
    items: List<TabItem<T>>,
    selectedIndex: Int,
    onTabSelected: (T, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .widthIn(min = 80.dp)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                    .clickable { onTabSelected(item.data, index) },
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = stringResource(item.titleRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75F)
                    },
                )
                if (item.isSmallIndicatorOn) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                            .size(4.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                    ) {}
                }
            }
        }
    }
}

data class TabItem<T : Any>(
    @StringRes val titleRes: Int,
    val isSmallIndicatorOn: Boolean,
    val data: T,
)
