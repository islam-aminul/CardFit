package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import `in`.firm.consultancy.bayaan.cardfit.data.scanner.MlKitDocumentScanner
import `in`.firm.consultancy.bayaan.cardfit.data.scanner.ScanSlot
import `in`.firm.consultancy.bayaan.cardfit.domain.model.ScannedSide
import `in`.firm.consultancy.bayaan.cardfit.ui.AppViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScreenScaffold
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

    ScreenScaffold(title = "Scan card") {
        // Hide the helper text once the front is captured, to free vertical space for Next on
        // small (≈5") screens.
        if (session?.front == null) {
            Text("Capture the front of the card. The back is optional.")
        }

        SideSection(
            label = "Front",
            side = session?.front,
            onCapture = { requestScan(ScanSlot.FRONT) },
        )
        SideSection(
            label = "Back (optional)",
            side = session?.back,
            onCapture = { requestScan(ScanSlot.BACK) },
        )

        errorMessage?.let { message ->
            Text(text = message, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = onNext,
            enabled = session?.front != null,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Next") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}

@Composable
private fun SideSection(
    label: String,
    side: ScannedSide?,
    onCapture: () -> Unit,
) {
    Text(text = label, style = MaterialTheme.typography.titleSmall)
    if (side == null) {
        Button(onClick = onCapture, modifier = Modifier.fillMaxWidth()) {
            Text("Capture $label")
        }
    } else {
        AsyncImage(
            model = side.imageUri,
            contentDescription = "$label preview",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        OutlinedButton(onClick = onCapture, modifier = Modifier.fillMaxWidth()) {
            Text("Retake $label")
        }
    }
}
