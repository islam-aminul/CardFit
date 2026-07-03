package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import `in`.firm.consultancy.bayaan.cardfit.data.scanner.MlKitDocumentScanner
import `in`.firm.consultancy.bayaan.cardfit.data.scanner.ScanSlot
import `in`.firm.consultancy.bayaan.cardfit.domain.model.ScannedSide
import `in`.firm.consultancy.bayaan.cardfit.ui.AppViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.BayaanCard
import `in`.firm.consultancy.bayaan.cardfit.ui.components.GhostButton
import `in`.firm.consultancy.bayaan.cardfit.ui.components.HelpText
import `in`.firm.consultancy.bayaan.cardfit.ui.components.PrimaryButton
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScaffoldBottomBar
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScreenScaffold
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Midnight200
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.TextHeading
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.TextMuted
import kotlinx.coroutines.launch

/**
 * Step 2 (CLAUDE.md section 11.2): scan the front, then optionally the back, using the on-device ML
 * Kit Document Scanner (its own capture/crop UI — no CameraX). Captured sides are persisted to files
 * and shown as thumbnails with a Retake action. CAMERA is requested at runtime before the first scan.
 */
@Composable
fun ScanScreen(
    viewModel: AppViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val scanner = remember { MlKitDocumentScanner(context.applicationContext) }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val session = state.session

    var currentSlot by remember { mutableStateOf(ScanSlot.FRONT) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> scope.launch {
                val side = scanner.persistFirstPage(result.data, currentSlot)
                if (side == null) {
                    errorMessage = "Couldn't read the scanned page. Please try again."
                } else {
                    when (currentSlot) {
                        ScanSlot.FRONT -> viewModel.setFront(side)
                        ScanSlot.BACK -> viewModel.setBack(side)
                    }
                }
            }
            // RESULT_CANCELED (or anything else): keep existing state, no error.
        }
    }

    fun launchScanner(slot: ScanSlot) {
        val act = activity
        if (act == null) {
            errorMessage = "Unable to start the scanner."
            return
        }
        currentSlot = slot
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
        if (granted) {
            launchScanner(currentSlot)
        } else {
            errorMessage = "Camera permission is required to scan."
        }
    }

    fun requestScan(slot: ScanSlot) {
        currentSlot = slot
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            launchScanner(slot)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    ScreenScaffold(
        title = "Scan card",
        bottomBar = {
            ScaffoldBottomBar {
                PrimaryButton(
                    onClick = onNext,
                    enabled = session?.front != null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Next") }
                GhostButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            }
        },
    ) {
        // Hide the helper text once the front is captured, to free vertical space for Next on
        // small (≈5") screens.
        if (session?.front == null) {
            HelpText("Capture the front of the card. The back is optional.")
        }

        ScanSlotCard(
            label = "Front",
            required = true,
            side = session?.front,
            onCapture = { requestScan(ScanSlot.FRONT) },
        )
        ScanSlotCard(
            label = "Back",
            required = false,
            side = session?.back,
            onCapture = { requestScan(ScanSlot.BACK) },
        )

        errorMessage?.let { message ->
            Text(text = message, color = MaterialTheme.colorScheme.error)
        }
    }
}

/** One scan slot: white bordered card with label + Required/Optional, thumbnail or dashed placeholder. */
@Composable
private fun ScanSlotCard(
    label: String,
    required: Boolean,
    side: ScannedSide?,
    onCapture: () -> Unit,
) {
    BayaanCard(contentPadding = PaddingValues(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                color = TextHeading,
            )
            Text(
                if (required) "Required" else "Optional",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = TextMuted,
            )
        }
        Spacer(Modifier.height(10.dp))
        if (side == null) {
            NotScannedPlaceholder()
        } else {
            AsyncImage(
                model = side.imageUri,
                contentDescription = "$label preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.585f)
                    .clip(RoundedCornerShape(8.dp)),
            )
        }
        Spacer(Modifier.height(10.dp))
        if (side == null) {
            PrimaryButton(onClick = onCapture, modifier = Modifier.fillMaxWidth()) {
                Text("Scan ${label.lowercase()}")
            }
        } else {
            GhostButton(onClick = onCapture, modifier = Modifier.fillMaxWidth()) {
                Text("Retake")
            }
        }
    }
}

/** Empty slot: card-ratio box with a 2dp dashed midnight-200 rounded border. */
@Composable
private fun NotScannedPlaceholder() {
    val density = LocalDensity.current
    val strokeWidth = with(density) { 2.dp.toPx() }
    val dash = with(density) { 12.dp.toPx() }
    val gap = with(density) { 8.dp.toPx() }
    val corner = with(density) { 8.dp.toPx() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.585f)
            .drawBehind {
                drawRoundRect(
                    color = Midnight200,
                    cornerRadius = CornerRadius(corner, corner),
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, gap), 0f),
                    ),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text("Not scanned yet", style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}
