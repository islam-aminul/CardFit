package `in`.firm.consultancy.bayaan.cardfit.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.firm.consultancy.bayaan.cardfit.ui.TaskViewModel
import `in`.firm.consultancy.bayaan.cardfit.ui.components.ScreenScaffold

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

    ScreenScaffold(title = "Tasks") {
        Text("Group several people's documents into one application set, then export them together.")

        Button(onClick = { showCreate = true }, modifier = Modifier.fillMaxWidth()) { Text("New task") }

        if (tasks.isEmpty()) {
            Text(
                "No tasks yet. Create one to get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        tasks.forEach { task ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.openTask(task.id)
                        onOpenTask()
                    },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(task.name.ifBlank { "(untitled task)" }, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${task.documents.size} document(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { pendingDelete = task.id }) { Text("Delete") }
                }
            }
        }

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
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
            title = { Text("Delete task?") },
            text = { Text("This removes the task and all of its captured images from the device.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTask(id)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
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
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }, enabled = text.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
