package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.firm.consultancy.bayaan.cardfit.ui.TaskViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.BayaanCard
import `in`.firm.consultancy.bayaan.cardfit.ui.components.BayaanTextField
import `in`.firm.consultancy.bayaan.cardfit.ui.components.GhostButton
import `in`.firm.consultancy.bayaan.cardfit.ui.components.GhostButtonSmall
import `in`.firm.consultancy.bayaan.cardfit.ui.components.HelpText
import `in`.firm.consultancy.bayaan.cardfit.ui.components.PrimaryButton
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScreenScaffold
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.Ink
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.TextHeading
import `in`.firm.consultancy.bayaan.cardfit.ui.theme.TextMuted

/**
 * Task mode home (CLAUDE.md Phase 14): list saved tasks (persisted, survive restarts), create a new
 * one, open one to edit, or delete it (which removes its image files).
 */
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    onOpenTask: () -> Unit,
    onBack: () -> Unit,
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    ScreenScaffold(title = "Tasks", onBack = onBack) {
        Text("Group several people's documents into one application set, then export them together.")

        PrimaryButton(onClick = { showCreate = true }, modifier = Modifier.fillMaxWidth()) { Text("New task") }

        if (tasks.isEmpty()) {
            HelpText("No tasks yet. Create one to get started.")
        }

        tasks.forEach { task ->
            BayaanCard(
                onClick = {
                    viewModel.openTask(task.id)
                    onOpenTask()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            task.name.ifBlank { "(untitled task)" },
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp),
                            color = TextHeading,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${task.documents.size} document(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                    }
                    GhostButtonSmall(onClick = { pendingDelete = task.id }) { Text("Delete") }
                }
            }
        }
    }

    if (showCreate) {
        NameDialog(
            title = "New task",
            label = "Task name",
            initial = "",
            confirmLabel = "Create",
            onDismiss = { showCreate = false },
            onConfirm = { name ->
                showCreate = false
                viewModel.createTask(name) { onOpenTask() }
            },
        )
    }

    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White,
            titleContentColor = TextHeading,
            textContentColor = Ink,
            title = { Text("Delete task?", style = MaterialTheme.typography.titleMedium) },
            text = { Text("This removes the task and all of its captured images from the device.") },
            confirmButton = {
                PrimaryButton(onClick = {
                    viewModel.deleteTask(id)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { GhostButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}

/** Small reusable single-field name dialog. */
@Composable
fun NameDialog(
    title: String,
    label: String,
    initial: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White,
        titleContentColor = TextHeading,
        textContentColor = Ink,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = {
            BayaanTextField(
                value = text,
                onValueChange = { text = it },
                label = label,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            PrimaryButton(onClick = { onConfirm(text.trim()) }, enabled = text.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = { GhostButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
