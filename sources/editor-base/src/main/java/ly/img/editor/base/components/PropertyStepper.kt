package ly.img.editor.base.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ly.img.editor.core.ui.UiDefaults

private const val STACKED_FONT_SCALE = 1.5f

/**
 * A titled [NumberStepperField] in a card. Its arguments match [PropertySlider]'s, plus a required [step], so a call
 * site converts by changing the name. The row shows the title, so a caller adds no [SectionHeader].
 *
 * @param onValueChangeFinished called once per press, hold, typed edit or TalkBack adjustment.
 */
@Composable
fun PropertyStepper(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    unit: String? = null,
    decimals: Int? = null,
    enabled: Boolean = true,
) {
    Card(colors = UiDefaults.cardColors) {
        PropertyStepperRow(title, value, onValueChange, onValueChangeFinished, valueRange, step, unit, decimals, enabled)
    }
}

/** The row of [PropertyStepper] without its card, for a section that already has one. */
@Composable
internal fun PropertyStepperRow(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    unit: String? = null,
    decimals: Int? = null,
    enabled: Boolean = true,
) {
    val label = @Composable {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
    val field = @Composable {
        NumberStepperField(
            value = value,
            onValueChange = onValueChange,
            title = title,
            valueRange = valueRange,
            step = step,
            unit = unit,
            decimals = decimals,
            enabled = enabled,
            onEditingChanged = { isEditing -> if (!isEditing) onValueChangeFinished() },
        )
    }
    // At large font sizes the field fills the row, so the title goes above it.
    if (LocalDensity.current.fontScale >= STACKED_FONT_SCALE) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            label()
            field()
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) { label() }
            field()
        }
    }
}
