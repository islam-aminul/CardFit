package `in`.firm.consultancy.bayaan.cardfit.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.firm.consultancy.bayaan.cardfit.domain.model.OutputMode
import `in`.firm.consultancy.bayaan.cardfit.domain.model.RenderConfig

/** A small, non-interactive chip summarising one output file, e.g. "Print · A4 · PDF". */
@Composable
fun OutputChip(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/** The chip label for one resolved output: "{mode} · {paper} · {format}", e.g. "Upload · A4 · JPEG". */
fun outputChipLabel(config: RenderConfig): String =
    "${config.mode.displayLabel()} · ${config.paper.name} · ${config.format.name}"

private fun OutputMode.displayLabel(): String = when (this) {
    OutputMode.PRINT -> "Print"
    OutputMode.UPLOAD -> "Upload"
}
