package `in`.firm.consultancy.bayaan.cardfit.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.BorderSubtle

/**
 * Bayaan step-screen scaffold: paper-background top bar with the Space Grotesk step title over a 1px
 * hairline, a scrollable 16dp-padded content column with 12dp gaps, and a pinned bottom action bar
 * (white, hairline on top) for the primary/Back pills. [scrollState] can be hoisted by the caller
 * (e.g. to auto-scroll on success).
 *
 * [bottomBar] is pinned below the scrolling content — used to anchor a "Back" (or primary action) to
 * the bottom of the screen without forcing the user to scroll to reach it. Prefer [onBack] for the
 * common case of a single anchored Back button.
 */
@Composable
fun ScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    onBack: (() -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val resolvedBottomBar: (@Composable () -> Unit)? = bottomBar ?: onBack?.let { back ->
        {
            ScaffoldBottomBar {
                GhostButton(onClick = back, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { BayaanTopBar(title) },
        bottomBar = { resolvedBottomBar?.invoke() },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/** Pinned bottom action bar: white surface with a 1px midnight-100 hairline on top. */
@Composable
fun ScaffoldBottomBar(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Color.White) {
        Column {
            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )
        }
    }
}
