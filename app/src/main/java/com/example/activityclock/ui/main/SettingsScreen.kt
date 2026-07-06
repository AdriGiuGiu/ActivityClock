package com.example.activityclock.ui.main

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.activityclock.data.ActivityRepository
import com.example.activityclock.data.SettingsRepository
import com.example.activityclock.viewmodel.ClockViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settingsRepo = remember { SettingsRepository(context) }
    
    val isDarkTheme by settingsRepo.isDarkTheme.collectAsState()
    val is24Hour by settingsRepo.is24HourFormat.collectAsState()
    val isMondayFirst by settingsRepo.isMondayFirst.collectAsState()
    
    val clockViewModel: ClockViewModel = viewModel {
        ClockViewModel(
            ActivityRepository(context.applicationContext),
            SettingsRepository(context.applicationContext)
        )
    }

    var showClearLogsDialog by remember { mutableStateOf(false) }
    var showClearActivitiesDialog by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    
    // Document launcher for exporting CSV
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val csvContent = clockViewModel.generateCsvExport()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(csvContent.toByteArray())
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // Preferences
            Text("Preferences", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column {
                    // Theme
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Dark Theme", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Enable obsidian dark mode", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { settingsRepo.setDarkTheme(it) }
                        )
                    }
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    
                    // Time Format
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("24-Hour Time", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Use 24-hour format in timeline", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = is24Hour,
                            onCheckedChange = { settingsRepo.set24HourFormat(it) }
                        )
                    }
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    
                    // Week Start
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Week Starts on Monday", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Affects the 'Week' analytics filter", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isMondayFirst,
                            onCheckedChange = { settingsRepo.setMondayFirst(it) }
                        )
                    }
                }
            }
            
            // Data Management
            Text("Data & Export", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            
            Button(
                onClick = { 
                    val fileName = "activity_clock_export_${System.currentTimeMillis()}.csv"
                    createDocumentLauncher.launch(fileName)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Logs to CSV")
            }
            
            Button(
                onClick = { showClearLogsDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear All Activity History")
            }

            Button(
                onClick = { showClearActivitiesDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete All Activities & Data")
            }
            
            // About App
            Text("About", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/AdriGiuGiu/ActivityClock"))
                    context.startActivity(intent)
                },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Info, 
                        contentDescription = "GitHub",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Activity Clock", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Created by AdriGiuGiu", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Tap to visit GitHub repository", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showClearLogsDialog) {
        AlertDialog(
            onDismissRequest = { showClearLogsDialog = false },
            title = { Text("Clear History?") },
            text = { Text("Are you sure you want to delete all recorded time logs? Your activity types will remain.") },
            confirmButton = {
                TextButton(onClick = { 
                    clockViewModel.clearAllLogs()
                    showClearLogsDialog = false 
                }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearLogsDialog = false }) { Text("Cancel") }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    
    if (showClearActivitiesDialog) {
        AlertDialog(
            onDismissRequest = { showClearActivitiesDialog = false },
            title = { Text("Delete Everything?") },
            text = { Text("Are you sure you want to delete ALL activities and history? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { 
                    clockViewModel.clearAllActivities()
                    showClearActivitiesDialog = false 
                }) { Text("Delete All", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearActivitiesDialog = false }) { Text("Cancel") }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
