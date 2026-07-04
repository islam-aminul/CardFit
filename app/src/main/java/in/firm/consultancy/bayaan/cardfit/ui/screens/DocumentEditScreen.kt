package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.firm.consultancy.bayaan.cardfit.data.photo.AndroidPhotoProcessor
import `in`.firm.consultancy.bayaan.cardfit.data.photo.NoBackgroundSegmenter
import `in`.firm.consultancy.bayaan.cardfit.data.scanner.MlKitDocumentScanner
import `in`.firm.consultancy.bayaan.cardfit.domain.PhotoEditParams
import `in`.firm.consultancy.bayaan.cardfit.ui.AppViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.GhostButton
import `in`.firm.consultancy.bayaan.cardfit.ui.components.HelpText
import `in`.firm.consultancy.bayaan.cardfit.ui.components.PrimaryButton
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScaffoldBottomBar
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScreenScaffold
import `in`.firm.consultancy.bayaan.cardfit.ui.components.SectionLabel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.bayaanSwitchColors
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Ink
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Midnight50
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.TextHeading
import kotlinx.coroutines.launch
import java.util.UUID

private const val PREVIEW_DIM = 1400

/**
 * Per-page document editor: rotate, auto-enhance (clean), and brightness/contrast/saturation, with a
 * live preview. Corner/perspective cropping is handled by the ML Kit scanner at capture (retake to
 * re-crop), so the in-app editor focuses on orientation and tone. Reuses the photo edit pipeline
 * ([AndroidPhotoProcessor]) with background removal disabled. On save, the page's [PhotoEditParams]
 * are stored non-destructively; the export renderer bakes them in.
 */
@Composable
fun DocumentEditScreen(
    viewModel: AppViewModel,
    pageIndex: Int,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val scanner = remember { MlKitDocumentScanner(context.applicationContext) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val page = state.session?.documentPages?.getOrNull(pageIndex)
    val sourceUri = page?.side?.imageUri

    val processor = remember { AndroidPhotoProcessor(context.applicationContext, NoBackgroundSegmenter) }

    // Re-scan to adjust corners / crop / perspective (ML Kit's editor), replacing this page's image
    // while keeping the current filter edits.
    val rescanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                val newSide = scanner.persistPage(result.data, "page-${UUID.randomUUID().toString().take(8)}")
                val current = viewModel.state.value.session?.documentPages?.getOrNull(pageIndex)
                if (newSide != null && current != null) {
                    viewModel.replaceDocumentPage(pageIndex, current.copy(side = newSide))
                }
            }
        }
    }
    fun launchRescan() {
        val act = activity ?: return
        scanner.startScanIntent(act)
            .addOnSuccessListener { rescanLauncher.launch(IntentSenderRequest.Builder(it).build()) }
    }
    val rescanPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) launchRescan() }
    fun requestRescan() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) launchRescan() else rescanPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Keyed on the source image (unique per capture), so a fresh scan at this index — or switching to a
    // different document type — resets the controls to that page's own edits instead of leaking the
    // previous image's filter values.
    var rotation by remember(sourceUri) { mutableStateOf(page?.edit?.rotationDegrees ?: 0) }
    var autoEnhance by remember(sourceUri) { mutableStateOf(page?.edit?.autoEnhance ?: true) }
    var brightness by remember(sourceUri) { mutableStateOf(page?.edit?.brightnessPercent ?: 0) }
    var contrast by remember(sourceUri) { mutableStateOf(page?.edit?.contrastPercent ?: 0) }
    var saturation by remember(sourceUri) { mutableStateOf(page?.edit?.saturationPercent ?: 0) }

    val params = PhotoEditParams(
        rotationDegrees = rotation,
        brightnessPercent = brightness,
        contrastPercent = contrast,
        saturationPercent = saturation,
        autoEnhance = autoEnhance,
    )

    // Live preview: re-process whenever params change (off the main thread via the suspend processor).
    val preview by produceState<Bitmap?>(initialValue = null, sourceUri, params) {
        value = null
        val uri = sourceUri ?: return@produceState
        value = processor.process(uri, params, PREVIEW_DIM)
    }

    ScreenScaffold(
        title = "Adjust & filters",
        bottomBar = {
            ScaffoldBottomBar {
                PrimaryButton(
                    onClick = {
                        if (page != null) viewModel.replaceDocumentPage(pageIndex, page.copy(edit = params))
                        onDone()
                    },
                    enabled = page != null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Save changes") }
                GhostButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
        },
    ) {
        if (page == null) {
            Text("No page to edit.")
            return@ScreenScaffold
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Midnight50),
            contentAlignment = Alignment.Center,
        ) {
            val shown = preview
            if (shown != null) {
                Image(
                    bitmap = shown.asImageBitmap(),
                    contentDescription = "Page preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(280.dp).padding(8.dp),
                )
            } else {
                CircularProgressIndicator()
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            GhostButton(onClick = { rotation = (rotation + 90) % 360 }, modifier = Modifier.weight(1f)) {
                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Rotate")
            }
            GhostButton(
                onClick = { rotation = 0; brightness = 0; contrast = 0; saturation = 0; autoEnhance = true },
                modifier = Modifier.weight(1f),
                contentColor = MaterialTheme.colorScheme.error,
            ) { Text("Reset") }
        }

        // Corner/crop and perspective are handled by the scanner: re-scan to recapture this page.
        GhostButton(onClick = { requestRescan() }, modifier = Modifier.fillMaxWidth()) {
            Text("Retake (adjust corners / crop)")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Auto-enhance (clean)", style = MaterialTheme.typography.bodyLarge, color = Ink)
            Switch(checked = autoEnhance, onCheckedChange = { autoEnhance = it }, colors = bayaanSwitchColors())
        }

        SectionLabel("Filters")
        AdjustRow("Brightness", brightness) { brightness = it }
        AdjustRow("Contrast", contrast) { contrast = it }
        AdjustRow("Saturation", saturation) { saturation = it }

        HelpText("Each filter applies to the original scan — adjusting one never stacks on another.")
    }
}

/** A −100..100 slider that commits on release so the preview re-runs once per adjustment. */
@Composable
private fun AdjustRow(label: String, value: Int, onCommit: (Int) -> Unit) {
    var local by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Ink)
            Text(local.toInt().toString(), style = MaterialTheme.typography.bodyMedium, color = TextHeading)
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onCommit(local.toInt()) },
            valueRange = -100f..100f,
            modifier = Modifier.heightIn(min = 32.dp),
        )
    }
}
