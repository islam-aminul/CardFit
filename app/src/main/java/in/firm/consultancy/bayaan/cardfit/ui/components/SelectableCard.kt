package `in`.firm.consultancy.bayaan.cardfit.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.AccentSoft
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Midnight50
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Midnight600
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Midnight800
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Teal500

/**
 * A small, compact tappable chip-card for multi-select choices (.bv-selectable): tonal midnight-50 at
 * rest; teal 2px border + teal/10 tint + midnight-800 medium text when [selected]; dims to 40% and
 * ignores taps when not [enabled] (e.g. a paper size beyond the max).
 */
@Composable
fun SelectableCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val container = when {
        !enabled -> Midnight50.copy(alpha = 0.4f)
        selected -> AccentSoft
        else -> Midnight50
    }
    val content = when {
        !enabled -> Midnight600.copy(alpha = 0.4f)
        selected -> Midnight800
        else -> Midnight600
    }
    // A transparent 2dp border at rest keeps the layout stable when selection adds the teal ring.
    val border = BorderStroke(2.dp, if (selected) Teal500 else Color.Transparent)
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = container,
        contentColor = content,
        border = border,
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}
