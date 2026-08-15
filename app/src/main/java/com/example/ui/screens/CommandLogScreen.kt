package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.AdbApplication
import com.example.ui.viewmodels.CommandViewModel
import com.example.ui.viewmodels.CommandViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

val COMMON_COMMANDS = listOf(
    "adb devices", "adb shell", "adb logcat", "adb push", "adb pull", 
    "adb install", "adb uninstall", "adb reboot", "adb reboot bootloader", 
    "adb kill-server", "adb tcpip 5555", "adb connect",
    "fastboot devices", "fastboot flash", "fastboot boot", 
    "fastboot oem unlock", "fastboot reboot", "fastboot flash recovery",
    "ls -l", "cd", "pwd", "cat", "grep", "ping", "ifconfig", "netstat", 
    "top", "ps", "df -h", "du -sh", "chmod", "chown", 
    "getprop", "setprop", "logcat -d", "dmesg", "clear"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandLogScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dao = (context.applicationContext as AdbApplication).database.commandHistoryDao()
    val viewModel: CommandViewModel = viewModel(factory = CommandViewModelFactory(dao))
    
    val history by viewModel.history.collectAsState()

    var command by remember { mutableStateOf("") }
    val logs = remember { mutableStateListOf<String>() }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    var showHistorySheet by remember { mutableStateOf(false) }

    val suggestions = remember(command) {
        if (command.isBlank()) emptyList()
        else {
            val lowerCommand = command.lowercase()
            COMMON_COMMANDS.filter { 
                it.lowercase().startsWith(lowerCommand) && !it.equals(command, ignoreCase = true)
            }.take(5)
        }
    }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Local Terminal", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Executes commands in the local Android shell (/system/bin/sh).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { showHistorySheet = true }) {
                Icon(Icons.Default.History, contentDescription = "Command History")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        // Log Viewer
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium)
                .padding(8.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                items(logs) { log ->
                    Text(
                        text = log,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (log.startsWith("$ ")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Suggestions
        if (suggestions.isNotEmpty()) {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suggestions) { suggestion ->
                    SuggestionChip(
                        onClick = { command = suggestion },
                        label = { Text(suggestion) }
                    )
                }
            }
        }
        
        // Input Area
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Enter command (e.g. ls, getprop)...") },
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { 
                    if (command.isNotBlank()) {
                        val cmdToRun = command
                        logs.add("$ $cmdToRun")
                        command = ""
                        viewModel.addCommand(cmdToRun)
                        coroutineScope.launch {
                            executeCommand(cmdToRun) { outputLine ->
                                logs.add(outputLine)
                            }
                        }
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send Command", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
    
    if (showHistorySheet) {
        ModalBottomSheet(onDismissRequest = { showHistorySheet = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Command History", style = MaterialTheme.typography.titleMedium)
                    if (history.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearHistory() }) {
                            Text("Clear", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (history.isEmpty()) {
                    Text(
                        "No commands executed yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn {
                        items(history) { historyItem ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        command = historyItem.command
                                        showHistorySheet = false
                                    }
                                    .padding(vertical = 12.dp)
                            ) {
                                Text(
                                    text = historyItem.command,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

suspend fun executeCommand(command: String, onOutput: (String) -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            // Use sh -c to allow chaining, pipes, etc.
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(true)
                .start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                withContext(Dispatchers.Main) {
                    onOutput(line ?: "")
                }
            }
            process.waitFor()
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onOutput("Error: ${e.message}")
            }
        }
    }
}

