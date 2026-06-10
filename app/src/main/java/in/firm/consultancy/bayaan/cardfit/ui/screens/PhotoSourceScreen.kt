package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import `in`.firm.consultancy.bayaan.cardfit.ui.PhotoViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScreenScaffold
import java.io.File

/**
 * Photo flow step 1 (CLAUDE.md Phase 13): obtain the source image by capturing with the camera or
 * picking from the gallery. The gallery uses the system photo picker (no storage permission); the
 * camera writes to a private FileProvider URI and requests CAMERA at runtime.
 */
@Composable
fun PhotoSourceScreen(
    viewModel: PhotoViewModel,
    onPicked: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // Pending camera-output URI string, remembered across the capture round-trip.
    var pendingCameraUri by remember { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.setSource(uri.toString())
            onPicked()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            viewModel.setSource(uri)
            onPicked()
        } else {
            errorMessage = "No photo was captured."
        }
    }

    fun launchCamera() {
        errorMessage = null
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "capture-${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingCameraUri = uri.toString()
        cameraLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) launchCamera() else errorMessage = "Camera permission is required to take a photo."
    }

    fun requestCamera() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) launchCamera() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    ScreenScaffold(title = "Add a photo") {
        Text("Take a new photo or choose one from your gallery. The original is never changed.")

        Button(onClick = { requestCamera() }, modifier = Modifier.fillMaxWidth()) {
            Text("Take photo")
        }
        Button(
            onClick = {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Choose from gallery") }

        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}
