package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.firm.consultancy.bayaan.cardfit.domain.PhotoSize
import `in`.firm.consultancy.bayaan.cardfit.ui.PhotoViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.CustomSizeDialog
import `in`.firm.consultancy.bayaan.cardfit.ui.components.PhotoCropFrame
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScaffoldBottomBar
import java.io.File

/**
 * Photo flow step 2 (CLAUDE.md Phase 13): the single editing page. The photo frame is pinned at the
 * top so every adjustment is visible live; the controls scroll beneath it. The photo shows at its true
 * aspect inside a fixed square frame with a centred crop aperture locked to the chosen size — pinch to
 * zoom and drag to position the face. Order: press-and-hold Compare, Rotate/Revert, Remove background
 * & Auto-enhance, photo-size cards, then Advanced (brightness/contrast/saturation). The source file is
 * never modified.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditScreen(
    viewModel: PhotoViewModel,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    val comparePreview by viewModel.comparePreview.collectAsStateWithLifecycle()
    val busy by viewModel.previewBusy.collectAsStateWithLifecycle()

    var compare by remember { mutableStateOf(false) }
    var showCustom by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    // Bumped to force the crop frame to recentre (e.g. after Revert or a fresh capture).
    var resetKey by remember { mutableIntStateOf(0) }

    val scrollState = rememberScrollState()

    // When Advanced expands, wait a frame for the sliders to measure, then reveal them.
    LaunchedEffect(showAdvanced) {
        if (showAdvanced) {
            withFrameNanos {}
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    // --- in-place recapture (camera / gallery) ---
    var pendingCameraUri by remember { mutableStateOf<String?>(null) }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { viewModel.setSource(uri.toString()); resetKey++ }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) { viewModel.setSource(uri); resetKey++ }
    }
    fun launchCamera() {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "capture-${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingCameraUri = uri.toString()
        cameraLauncher.launch(uri)
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) launchCamera() }
    fun requestCamera() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) launchCamera() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val cropAspect = state.resolvedSize?.aspectRatio?.toFloat() ?: (7f / 9f)
    val imageAspect = state.previewAspect()
    val shown = (if (compare) comparePreview else preview)?.asImageBitmap()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Edit photo") }) },
        bottomBar = {
            ScaffoldBottomBar {
                Button(
                    onClick = onNext,
                    enabled = state.resolvedSize != null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Next") }
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // --- pinned photo frame (always visible) ---
            PhotoCropFrame(
                image = shown,
                imageAspect = imageAspect,
                cropAspect = cropAspect,
                resetKey = resetKey,
                busy = busy,
                onCrop = viewModel::setCropNorm,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = { requestCamera() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Retake")
                }
                TextButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Gallery")
                }
            }

            // --- scrolling controls ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HoldCompareButton(active = compare, onHoldChange = { compare = it }, modifier = Modifier.fillMaxWidth())

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { viewModel.rotateClockwise() }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Rotate")
                    }
                    OutlinedButton(
                        onClick = { viewModel.revertEdits(); resetKey++ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Revert") }
                }

                ToggleRow("Remove background (white)", state.removeBackground, viewModel::setRemoveBackground)
                if (state.removeBackground) {
                    Text(
                        "Quality varies on hair and edges — turn off to revert.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ToggleRow("Auto-enhance", state.autoEnhance, viewModel::setAutoEnhance)

                HorizontalDivider()

                Text("Photo size", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PhotoSize.entries.forEach { size ->
                        PhotoSizeCard(
                            label = labelFor(size),
                            aspect = aspectFor(size, state.customWidthMm, state.customHeightMm),
                            selected = state.size == size,
                            onClick = {
                                if (size == PhotoSize.CUSTOM) showCustom = true else viewModel.selectSize(size)
                            },
                        )
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdvanced = !showAdvanced }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Advanced options", style = MaterialTheme.typography.bodyLarge)
                    Icon(
                        if (showAdvanced) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (showAdvanced) "Collapse" else "Expand",
                    )
                }
                if (showAdvanced) {
                    AdjustSlider("Brightness", state.brightnessPercent, viewModel::setBrightness)
                    AdjustSlider("Contrast", state.contrastPercent, viewModel::setContrast)
                    AdjustSlider("Saturation", state.saturationPercent, viewModel::setSaturation)
                }

                Text(
                    "Everything is processed on your device — nothing is uploaded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showCustom) {
        CustomSizeDialog(
            onDismiss = { showCustom = false },
            onConfirm = { widthMm, heightMm ->
                viewModel.setCustomSizeMm(widthMm, heightMm)
                showCustom = false
            },
            initialWidthMm = state.customWidthMm ?: 35.0,
            initialHeightMm = state.customHeightMm ?: 45.0,
        )
    }
}

/**
 * Outlined, button-styled surface that reports its pressed state. Held → show the original; released →
 * back to the edited preview. Press detection lives on its own composable (not layered over the crop
 * gestures), so it stays reliable.
 */
@Composable
private fun HoldCompareButton(active: Boolean, onHoldChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        contentColor = if (active) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.primary
        },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(onPress = {
                onHoldChange(true)
                tryAwaitRelease()
                onHoldChange(false)
            })
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(if (active) "Showing original — release" else "Press and hold to compare")
        }
    }
}

/** A small, equal-sized size card: a proportioned rectangle (the size's aspect) with a portrait icon. */
@Composable
private fun PhotoSizeCard(label: String, aspect: Float, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.width(84.dp).clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(modifier = Modifier.height(52.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(aspect)
                        .border(1.5.dp, LocalContentColorOf(selected), RoundedCornerShape(3.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
private fun LocalContentColorOf(selected: Boolean): Color =
    if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/**
 * A -100..100 adjustment slider that updates a smooth local value while dragging and commits to the
 * ViewModel only when the gesture ends (so the edit pipeline re-runs once per change, not per tick).
 */
@Composable
private fun AdjustSlider(label: String, value: Int, onCommit: (Int) -> Unit) {
    var local by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(local.toInt().toString(), style = MaterialTheme.typography.bodyMedium)
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

private fun labelFor(size: PhotoSize): String = when (size) {
    PhotoSize.PASSPORT_INDIA -> "Passport"
    PhotoSize.VISA -> "Visa"
    PhotoSize.STAMP -> "Stamp"
    PhotoSize.CUSTOM -> "Custom"
}

private fun aspectFor(size: PhotoSize, customW: Double?, customH: Double?): Float {
    val a = size.aspectRatio ?: if (customW != null && customH != null && customH > 0) customW / customH else 1.0
    return a.toFloat().coerceIn(0.5f, 1.5f)
}
