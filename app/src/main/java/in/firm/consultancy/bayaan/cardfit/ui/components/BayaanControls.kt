package `in`.firm.consultancy.bayaan.cardfit.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.AccentSoft
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.BorderSubtle
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.ErrorRed
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Ink
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Midnight200
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Midnight600
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Midnight800
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Paper
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Sage600
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Teal500
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Teal600
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Teal700
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.TextHeading
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.TextMuted

/**
 * Bayaan-styled primitives shared by every screen (design-system/components/components.css).
 * Buttons are pills; edges are 1px midnight-100 borders — never shadows; teal marks selection/focus.
 */

/** Pill radius for all buttons, tags, and accent bars (--radius-full). */
val PillShape: Shape = RoundedCornerShape(percent = 50)

/** Filled primary action (.bv-btn--primary): midnight-800 pill, white label; disabled = 40% opacity. */
@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Midnight800,
            contentColor = Color.White,
            disabledContainerColor = Midnight800.copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 0.8f),
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        modifier = modifier,
        content = content,
    )
}

/** Sage filled action (.bv-btn--sage): sage-600 pill, white label — the calm secondary "add" accent. */
@Composable
fun SageButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Sage600,
            contentColor = Color.White,
            disabledContainerColor = Sage600.copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 0.8f),
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        modifier = modifier,
        content = content,
    )
}

/** Outlined secondary action (.bv-btn--ghost): transparent pill, 1px midnight-200 border. */
@Composable
fun GhostButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = Midnight800,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = PillShape,
        border = BorderStroke(1.dp, if (enabled) Midnight200 else Midnight200.copy(alpha = 0.4f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor,
            disabledContentColor = contentColor.copy(alpha = 0.4f),
        ),
        contentPadding = PaddingValues(horizontal = 23.dp, vertical = 11.dp),
        modifier = modifier,
        content = content,
    )
}

/** Compact filled pill — the emphasised counterpart to [GhostButtonSmall] in an inline action row. */
@Composable
fun PrimaryButtonSmall(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Midnight800,
            contentColor = Color.White,
            disabledContainerColor = Midnight800.copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 0.8f),
        ),
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 7.dp),
        modifier = modifier.defaultMinSize(minWidth = 1.dp, minHeight = 32.dp),
    ) {
        androidx.compose.material3.ProvideTextStyle(MaterialTheme.typography.labelMedium) {
            content()
        }
    }
}

/** Compact ghost pill for inline row actions (e.g. Delete on a task row). */
@Composable
fun GhostButtonSmall(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = Midnight800,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = PillShape,
        border = BorderStroke(1.dp, if (enabled) Midnight200 else Midnight200.copy(alpha = 0.4f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor,
            disabledContentColor = contentColor.copy(alpha = 0.4f),
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        modifier = modifier.defaultMinSize(minWidth = 1.dp, minHeight = 32.dp),
    ) {
        androidx.compose.material3.ProvideTextStyle(MaterialTheme.typography.labelMedium) {
            content()
        }
    }
}

/**
 * Bayaan text field (.bv-field / AppTextField): uppercase tracked label ABOVE a white, 12px-radius
 * outlined input with a teal focus border; optional helper text below.
 */
@Composable
fun BayaanTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    help: String? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.08.em),
            color = if (isError) ErrorRed else Midnight600,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            isError = isError,
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Teal500,
                unfocusedBorderColor = Midnight200,
                errorBorderColor = ErrorRed,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                errorContainerColor = Color.White,
                disabledContainerColor = Color.White,
                cursorColor = Teal600,
                focusedTextColor = Ink,
                unfocusedTextColor = Ink,
                errorTextColor = Ink,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        if (help != null) HelpText(help)
    }
}

/** Section label: 14px Space Grotesk semibold in heading midnight. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = TextHeading, modifier = modifier)
}

/** Muted helper copy (12px Inter, midnight-600/70). */
@Composable
fun HelpText(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = modifier)
}

/** Bayaan switch (.bv-switch): teal-500 track when on, midnight-200 when off, white thumb, no ring. */
@Composable
fun bayaanSwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = Teal500,
    checkedBorderColor = Color.Transparent,
    uncheckedThumbColor = Color.White,
    uncheckedTrackColor = Midnight200,
    uncheckedBorderColor = Color.Transparent,
    disabledCheckedThumbColor = Color.White.copy(alpha = 0.8f),
    disabledCheckedTrackColor = Teal500.copy(alpha = 0.4f),
    disabledUncheckedThumbColor = Color.White.copy(alpha = 0.8f),
    disabledUncheckedTrackColor = Midnight200.copy(alpha = 0.4f),
)

/** White card with a 1px midnight-100 border (.bv-card). Borders, not shadows. */
@Composable
fun BayaanCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = Color.White,
    contentColor: Color = Ink,
    shape: Shape = RoundedCornerShape(16.dp),
    border: BorderStroke? = BorderStroke(1.dp, BorderSubtle),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            border = border,
            modifier = modifier,
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            border = border,
            modifier = modifier,
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    }
}

/** Teal-soft status callout (e.g. "Saved N file(s)"): rounded-10, teal tint, teal-700 text. */
@Composable
fun TealCallout(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = AccentSoft,
        contentColor = Teal700,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content,
        )
    }
}

/**
 * Small-document size on the page (25–100% of the maximum). Tracks a smooth local value while
 * dragging and commits when the gesture ends, so the (heavy) page render re-runs once per
 * adjustment. Shared by the Configure and Preview steps.
 */
@Composable
fun DocumentSizeSlider(percent: Int, onCommit: (Int) -> Unit, modifier: Modifier = Modifier) {
    var local by remember(percent) { mutableFloatStateOf(percent.toFloat()) }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel("Document size")
            Text(
                "${local.toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = TextHeading,
            )
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onCommit(local.toInt()) },
            valueRange = 25f..100f,
        )
    }
}

/** Bayaan app top bar: paper background, Space Grotesk step title, 1px hairline underneath. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BayaanTopBar(title: String) {
    Column {
        TopAppBar(
            title = { Text(title) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Paper,
                titleContentColor = TextHeading,
            ),
        )
        HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
    }
}
