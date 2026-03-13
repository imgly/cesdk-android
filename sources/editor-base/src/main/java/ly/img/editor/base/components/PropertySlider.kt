package ly.img.editor.base.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ly.img.editor.core.ui.UiDefaults

@Composable
fun PropertySlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    decimalPlaces: Int = 1,
) {
    Column {
        SectionHeader(text = title)
        var sliderValue by remember(value) { mutableStateOf(value) }
        Card(
            colors = UiDefaults.cardColors,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        sliderValue = it
                        onValueChange(it)
                    },
                    valueRange = valueRange,
                    steps = steps,
                    modifier = Modifier.weight(1f),
                    onValueChangeFinished = {
                        if (sliderValue != value) {
                            onValueChangeFinished()
                        }
                    },
                )
                Text(
                    text = String.format("%.${decimalPlaces}f", sliderValue),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(min = 40.dp).padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
fun PropertySlider(
    @StringRes title: Int,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    decimalPlaces: Int = 1,
) = PropertySlider(
    title = stringResource(title),
    value = value,
    onValueChange = onValueChange,
    onValueChangeFinished = onValueChangeFinished,
    valueRange = valueRange,
    steps = steps,
    decimalPlaces = decimalPlaces,
)
