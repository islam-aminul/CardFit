package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import `in`.firm.consultancy.bayaan.cardfit.data.scanner.MlKitDocumentScanner
import `in`.firm.consultancy.bayaan.cardfit.domain.DimensionUnit
import `in`.firm.consultancy.bayaan.cardfit.domain.PhotoEditParams
import `in`.firm.consultancy.bayaan.cardfit.domain.formatLength
import `in`.firm.consultancy.bayaan.cardfit.domain.model.CardType
import `in`.firm.consultancy.bayaan.cardfit.domain.model.DocumentPage
import `in`.firm.consultancy.bayaan.cardfit.ui.AppViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.SettingsViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.BayaanCard
import `in`.firm.consultancy.bayaan.cardfit.ui.components.GhostButton
import `in`.firm.consultancy.bayaan.cardfit.ui.components.GhostButtonSmall
import `in`.firm.consultancy.bayaan.cardfit.ui.components.HelpText
import `in`.firm.consultancy.bayaan.cardfit.ui.components.OutputChip
import `in`.firm.consultancy.bayaan.cardfit.ui.components.PrimaryButton
import `in`.firm.consultancy.bayaan.cardfit.ui.components.SageButton
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScaffoldBottomBar
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScreenScaffold
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Midnight50
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.TextHeading
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.TextMuted
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Document capture (FULL_PAGE_DOCUMENT / RECEIPT). Selecting the type launches the ML Kit scanner
 * directly (corner-adjust / rotate / retake / clean are built in); each captured page is auto-enhanced
 * and appended. The user can edit a page, reorder/remove it, and — when [allowMultiPage] — add more
 * pages (full-page = more same-size pages; receipt = more, possibly different-width, pieces). Receipts
 * collect a real-world width per page via [onPickReceiptWidth] before they can proceed.
 *
 * In the Application-set (task) flow [allowMultiPage] is false: exactly one page is captured and the
 * "Add" affordance is hidden.
 */
@Composable
fun DocumentPagesScreen(
    viewModel: AppViewModel,
    allowMultiPage: Boolean,
    onNext: () -> Unit,
    onEditPage: (Int) -> Unit,
    onPickReceiptWidth: (Int) -> Unit,
    onBack: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val unit by settingsViewModel.unit.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val scanner = remember { MlKitDocumentScanner(context.applicationContext) }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val session = state.session
    val isReceipt = session?.cardType == CardType.RECEIPT
    val pages = session?.documentPages ?: emptyList()

    var errorMessage by remember { mutableStateOf<String?>(null) }
    // Auto-launch the scanner once on first entry (empty page list) — "opens the camera directly".
    // Saved across config changes so rotating while the scanner is open doesn't relaunch it.
    var autoLaunched by rememberSaveable { mutableStateOf(false) }

    val scanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                val side = scanner.persistPage(result.data, "page-${UUID.randomUUID().toString().take(8)}")
                if (side == null) {
                    errorMessage = "Couldn't read the scanned page. Please try again."
                } else {
                    val wasEmpty = viewModel.state.value.session?.documentPages.isNullOrEmpty()
                    viewModel.addDocumentPage(DocumentPage(side, edit = PhotoEditParams(autoEnhance = true)))
                    // Mirror the first page into [front] so OCR name-suggestion + preview keep working.
                    if (wasEmpty) viewModel.setFront(side)
                    val newIndex = (viewModel.state.value.session?.documentPages?.size ?: 1) - 1
                    if (isReceipt) onPickReceiptWidth(newIndex)
                }
            }
        }
        // RESULT_CANCELED: keep existing state.
    }

    fun launchScanner() {
        val act = activity
        if (act == null) {
            errorMessage = "Unable to start the scanner."
            return
        }
        errorMessage = null
        scanner.startScanIntent(act)
            .addOnSuccessListener { intentSender ->
                scanLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { e ->
                errorMessage = "Scanner unavailable: ${e.localizedMessage ?: "unknown error"}"
            }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) launchScanner() else errorMessage = "Camera permission is required to scan."
    }

    fun requestScan() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) launchScanner() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Must run in an effect, never inline during composition: the launchers above are only registered
    // once the composition commits, so launching one from the composition pass throws
    // "Launcher has not been initialized" (hit on the permission-not-yet-granted path).
    LaunchedEffect(Unit) {
        if (!autoLaunched && pages.isEmpty()) {
            autoLaunched = true
            requestScan()
        }
    }

    val subject = if (isReceipt) "receipt" else "document"
    val nextEnabled = pages.isNotEmpty() && (!isReceipt || pages.all { it.widthMm != null })

    ScreenScaffold(
        title = if (isReceipt) "Receipt" else "Document",
        bottomBar = {
            ScaffoldBottomBar {
                if (allowMultiPage && pages.isNotEmpty()) {
                    SageButton(onClick = { requestScan() }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (isReceipt) "Add another" else "Add page")
                    }
                }
                PrimaryButton(onClick = onNext, enabled = nextEnabled, modifier = Modifier.fillMaxWidth()) {
                    Text("Next")
                }
                GhostButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            }
        },
    ) {
        if (pages.isEmpty()) {
            HelpText("Capture the $subject to continue.")
            PrimaryButton(onClick = { requestScan() }, modifier = Modifier.fillMaxWidth()) {
                Text("Scan $subject")
            }
        } else {
            HelpText(
                if (isReceipt) {
                    "Each receipt is placed on the sheet at the width you choose."
                } else {
                    "Each page prints on its own sheet. Add more pages for a multi-page document."
                },
            )
            pages.forEachIndexed { index, page ->
                DocumentPageRow(
                    index = index,
                    page = page,
                    isReceipt = isReceipt,
                    unit = unit,
                    isFirst = index == 0,
                    isLast = index == pages.lastIndex,
                    onEdit = { onEditPage(index) },
                    onSetWidth = { onPickReceiptWidth(index) },
                    onMoveUp = { viewModel.reorderDocumentPage(index, index - 1) },
                    onMoveDown = { viewModel.reorderDocumentPage(index, index + 1) },
                    onRemove = {
                        viewModel.removeDocumentPage(index)
                        // Keep [front] mirroring the first remaining page (or clear it).
                        val remaining = viewModel.state.value.session?.documentPages
                        viewModel.setFront(remaining?.firstOrNull()?.side)
                    },
                )
            }
        }

        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun DocumentPageRow(
    index: Int,
    page: DocumentPage,
    isReceipt: Boolean,
    unit: DimensionUnit,
    isFirst: Boolean,
    isLast: Boolean,
    onEdit: () -> Unit,
    onSetWidth: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    BayaanCard(contentPadding = PaddingValues(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Large page thumbnail (fills the tile width). The stored rotation is applied so a rotated
            // page shows correctly here without re-baking the source image.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Midnight50),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = page.side.imageUri,
                    contentDescription = if (isReceipt) "Receipt ${index + 1}" else "Page ${index + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(6.dp)
                        .rotate(page.edit.rotationDegrees.toFloat()),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Just the page number (kept short so it never wraps), plus a receipt's width chip.
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextHeading,
                    )
                    if (isReceipt) {
                        val w = page.widthMm
                        GhostButtonSmall(onClick = onSetWidth) {
                            Text(if (w != null) formatLength(w, unit) else "Set width")
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    GhostButtonSmall(onClick = onMoveUp, enabled = !isFirst) { Text("↑") }
                    GhostButtonSmall(onClick = onMoveDown, enabled = !isLast) { Text("↓") }
                    GhostButtonSmall(onClick = onEdit) { Text("Adjust") }
                    GhostButtonSmall(onClick = onRemove) { Text("✕") }
                }
            }
        }
    }
}
