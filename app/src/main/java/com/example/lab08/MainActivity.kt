package com.example.lab08

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.room.Room
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.lab08.ui.theme.Lab08Theme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Lab08Theme {
                val db = Room.databaseBuilder(
                    applicationContext,
                    TaskDatabase::class.java,
                    "task_db"
                ).build()
                val taskDao = db.taskDao()
                val viewModel = TaskViewModel(taskDao)
                TaskScreen(viewModel)
            }
        }
    }
}

// Programa un recordatorio que se dispara en 1 minuto (Ejercicio 1, función 6)
fun scheduleReminder(context: Context) {
    val request = OneTimeWorkRequestBuilder<ReminderWorker>()
        .setInitialDelay(1, TimeUnit.MINUTES)
        .build()
    WorkManager.getInstance(context).enqueue(request)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.visibleTasks.collectAsState()
    val currentFilter by viewModel.filter.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var newTaskDescription by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Pide el permiso de notificaciones (Android 13 o superior)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scheduleReminder(context)
            Toast.makeText(context, "Listo, te recordaré en 1 minuto", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Sin permiso no puedo mostrar recordatorios", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis tareas") },
                actions = {
                    IconButton(onClick = {
                        val needsPermission =
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) != PackageManager.PERMISSION_GRANTED
                        if (needsPermission) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            scheduleReminder(context)
                            Toast.makeText(context, "Listo, te recordaré en 1 minuto", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Programar recordatorio")
                    }
                    TextButton(onClick = { viewModel.deleteAllTasks() }) {
                        Text("Borrar todo")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar tarea")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Fichas de filtro
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = currentFilter == TaskFilter.ALL,
                    onClick = { viewModel.setFilter(TaskFilter.ALL) },
                    label = { Text("Todas") }
                )
                FilterChip(
                    selected = currentFilter == TaskFilter.PENDING,
                    onClick = { viewModel.setFilter(TaskFilter.PENDING) },
                    label = { Text("Pendientes") }
                )
                FilterChip(
                    selected = currentFilter == TaskFilter.COMPLETED,
                    onClick = { viewModel.setFilter(TaskFilter.COMPLETED) },
                    label = { Text("Completadas") }
                )
            }

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay tareas para mostrar")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(tasks, key = { it.id }) { task ->
                        TaskItem(
                            task = task,
                            onToggle = { viewModel.toggleTaskCompletion(task) },
                            onDelete = { viewModel.deleteTask(task) }
                        )
                    }
                }
            }
        }
    }

    // Diálogo para agregar una tarea nueva
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nueva tarea") },
            text = {
                TextField(
                    value = newTaskDescription,
                    onValueChange = { newTaskDescription = it },
                    label = { Text("Descripción") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTaskDescription.isNotBlank()) {
                        viewModel.addTask(newTaskDescription.trim())
                        newTaskDescription = ""
                        showAddDialog = false
                    }
                }) {
                    Text("Agregar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun TaskItem(task: Task, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle() })
        Text(
            text = task.description,
            modifier = Modifier.weight(1f),
            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Eliminar tarea")
        }
    }
}