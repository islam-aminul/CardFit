package `in`.firm.consultancy.bayaan.cardfit.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.AccentSoft
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Midnight50
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Midnight600
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Midnight800
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Teal500

/**
 * A selectable tile that, unlike the text-only [SelectableCard], shows a schematic OUTCOME
 * illustration above its label so the user can guess what the option produces (CLAUDE.md section 11:
 * "visual tiles, progressive disclosure"). Bayaan states (.bv-illustrated): tonal midnight-50 at
 * rest, teal 2px border + teal/10 tint when selected, 40% opacity when disabled.
 *
 * [artwork] receives the [accent] colour to draw with — the tile's current content colour, so the
 * illustrations from [ExportArtwork] read correctly on any state without theme access of their own.
 */
@Composable
fun IllustratedTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    artwork: @Composable (accent: Color) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    subtitle: String? = null,
    artHeight: Dp = 56.dp,
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
    // Transparent 2dp border at rest so the teal selection ring doesn't shift the layout.
    val border = BorderStroke(2.dp, if (selected) Teal500 else Color.Transparent)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = container,
        contentColor = content,
        border = border,
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(artHeight)) {
                artwork(content)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = content.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
