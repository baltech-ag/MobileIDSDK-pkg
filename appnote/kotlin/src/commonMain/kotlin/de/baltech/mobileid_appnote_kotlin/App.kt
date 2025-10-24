@file:OptIn(ExperimentalMaterial3Api::class)

package de.baltech.mobileid_appnote_kotlin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.baltech.mobileid.*
import kotlinx.coroutines.launch

fun isValidHex(input: String): Boolean {
    return input.length == 32 && input.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
}

@Composable
fun App() {
    val dataStore = remember { createDataStore() }
    val repository = remember { CredentialRepository(dataStore) }
    var currentScreen by remember { mutableStateOf(Screen.CREDENTIAL_LIST) }
    val activeCredential by repository.selectedCredential.collectAsState()
    val sdk = remember { MobileIdSdk() }
    var availability by remember { mutableStateOf(AvailabilityStates.UNDEFINED) }
    val scope = rememberCoroutineScope()
    var showRemoteTriggerDialog by remember { mutableStateOf(false) }
    var readers by remember { mutableStateOf<List<Reader>>(emptyList()) }

    // Calculate the index of the active credential
    val activeCredentialIndex = remember(activeCredential, repository.credentials.size) {
        activeCredential?.let { active ->
            repository.credentials.indexOfFirst { it == active }.takeIf { it >= 0 }
        }
    }

    LaunchedEffect(Unit) {
        sdk.onAvailabilityChange = { newAvailability ->
            availability = newAvailability
        }
        sdk.onReadersUpdate = {
            readers = sdk.readers
        }
    }

    LaunchedEffect(readers.isEmpty(), showRemoteTriggerDialog) {
        if (readers.isEmpty() && showRemoteTriggerDialog) {
            showRemoteTriggerDialog = false
        }
    }

    LaunchedEffect(readers.size) {
        if (readers.isNotEmpty() && !showRemoteTriggerDialog) {
            showRemoteTriggerDialog = true
        }
    }

    LaunchedEffect(activeCredentialIndex, repository.credentials.size) {
        scope.launch {
            val index = activeCredentialIndex
            if (index != null && index < repository.credentials.size) {
                try {
                    val activeCredential = repository.credentials[index]
                    val projectKey = activeCredential.projectKeyHex.hexToByteArray()
                    sdk.credentials = listOf(Credential.create(projectKey, activeCredential.credentialId))
                } catch (e: Exception) {
                    // Handle credential conversion error
                    repository.selectCredential(null)
                }
            } else {
                sdk.credentials = emptyList()
            }
        }
    }

    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    ) {
        when (currentScreen) {
            Screen.CREDENTIAL_LIST -> {
                CredentialListScreen(
                    credentials = repository.credentials,
                    activeCredentialIndex = activeCredentialIndex,
                    availability = availability,
                    sdk = sdk,
                    readers = readers,
                    onCredentialClick = { index ->
                        val credential = repository.credentials.getOrNull(index)
                        repository.selectCredential(if (activeCredentialIndex == index) null else credential)
                    },
                    onDeleteCredential = { index ->
                        val credential = repository.credentials[index]
                        repository.removeCredential(credential)
                    },
                    onAddCredential = {
                        currentScreen = Screen.ADD_CREDENTIAL
                    },
                    onNavigateToLogs = {
                        currentScreen = Screen.LOG_VIEWER
                    },
                    onOpenRemoteTrigger = {
                        showRemoteTriggerDialog = true
                    }
                )
                if (showRemoteTriggerDialog) {
                    RemoteTriggerDialog(
                        readers = readers,
                        onDismiss = { showRemoteTriggerDialog = false }
                    )
                }
            }
            Screen.ADD_CREDENTIAL -> {
                AddCredentialScreen(
                    onSave = { credential ->
                        repository.addCredential(credential)
                        currentScreen = Screen.CREDENTIAL_LIST
                    },
                    onCancel = {
                        currentScreen = Screen.CREDENTIAL_LIST
                    }
                )
            }
            Screen.LOG_VIEWER -> {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Mobile ID Logs") },
                            navigationIcon = {
                                IconButton(onClick = { currentScreen = Screen.CREDENTIAL_LIST }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        sdk.createLogView()
                    }
                }
            }
        }
    }
}

@Composable
fun RemoteTriggerDialog(
    readers: List<Reader>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remote Trigger") },
        text = {
            if (readers.isEmpty()) {
                Text(
                    "No readers available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(readers) { reader ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    reader.trigger()
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = reader.displayName,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun CredentialListScreen(
    credentials: List<CredentialData>,
    activeCredentialIndex: Int?,
    availability: Availability,
    sdk: MobileIdSdk,
    readers: List<Reader>,
    onCredentialClick: (Int) -> Unit,
    onDeleteCredential: (Int) -> Unit,
    onAddCredential: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onOpenRemoteTrigger: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mobile ID Appnote (Kotlin)") },
                navigationIcon = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                        DropdownMenuItem(
                            text = { Text("Remote Trigger") },
                            onClick = {
                                showMenu = false
                                onOpenRemoteTrigger()
                            },
                            enabled = activeCredentialIndex != null && readers.isNotEmpty()
                        )
                        DropdownMenuItem(
                            text = { Text("View Logs") },
                            onClick = {
                                showMenu = false
                                onNavigateToLogs()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.List, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Send Logs") },
                            onClick = {
                                showMenu = false
                                scope.launch {
                                    try {
                                        sdk.sendLogs()
                                    } catch (e: Exception) {
                                        // Handle error - could show a snackbar here
                                    }
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null)
                            }
                        )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (credentials.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No credentials added yet.\nTap + to add your first credential.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                FloatingActionButton(
                    onClick = onAddCredential,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Credential")
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(credentials) { index, credential ->
                            CredentialItem(
                                credential = credential,
                                isActive = activeCredentialIndex == index,
                                onClick = { onCredentialClick(index) },
                                onDelete = { onDeleteCredential(index) }
                            )
                        }
                    }

                    if (activeCredentialIndex == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Tap a credential to activate the Appnote's functionality",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(0.5f))
                    }

                    AvailabilityStatus(availability = availability, sdk = sdk)
                }
                FloatingActionButton(
                    onClick = onAddCredential,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 80.dp, end = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Credential")
                }
            }
        }
    }
}

@Composable
fun AvailabilityStatus(availability: Availability, sdk: MobileIdSdk) {
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = when (availability) {
            AvailabilityStates.OK -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            AvailabilityStates.DISABLED, AvailabilityStates.UNAUTHORIZED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            AvailabilityStates.UNSUPPORTED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            AvailabilityStates.PERMISSIONS_DENIED, AvailabilityStates.PERMISSIONS_REQUIRED, AvailabilityStates.PERMISSIONS_PERMANENTLY_DENIED -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Bluetooth Status: ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    when (availability) {
                        AvailabilityStates.PERMISSIONS_DENIED, AvailabilityStates.PERMISSIONS_REQUIRED -> "Permissions Required"
                        AvailabilityStates.PERMISSIONS_PERMANENTLY_DENIED -> "Permissions Denied"
                        else -> availability
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (availability) {
                        AvailabilityStates.OK -> MaterialTheme.colorScheme.primary
                        AvailabilityStates.DISABLED, AvailabilityStates.UNAUTHORIZED -> MaterialTheme.colorScheme.error
                        AvailabilityStates.UNSUPPORTED -> MaterialTheme.colorScheme.error
                        AvailabilityStates.PERMISSIONS_DENIED, AvailabilityStates.PERMISSIONS_REQUIRED, AvailabilityStates.PERMISSIONS_PERMANENTLY_DENIED -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            // Show permission action buttons when permissions are needed
            when (availability) {
                AvailabilityStates.PERMISSIONS_DENIED, AvailabilityStates.PERMISSIONS_REQUIRED -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                sdk.requestPermissions()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant Permissions")
                    }
                }
                AvailabilityStates.PERMISSIONS_PERMANENTLY_DENIED -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    sdk.openPermissionSettings()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Open Settings")
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    sdk.requestPermissions()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Try Again")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CredentialItem(
    credential: CredentialData,
    isActive: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val backgroundColor = if (isActive) {
        Color(0xFF0066CC) // Darker blue for active
    } else {
        Color(0xFFB3D9FF) // Light blue for inactive
    }

    val textColor = if (isActive) {
        Color.White
    } else {
        Color(0xFF003366) // Dark blue
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(224.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Project key and ID at bottom right
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = credential.projectKeyHex,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.8f)
                )
                Text(
                    text = credential.credentialId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            // Delete icon at top right
            IconButton(
                onClick = { onDelete() },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Credential",
                    tint = textColor
                )
            }
        }
    }
}

@Composable
fun AddCredentialScreen(
    onSave: (CredentialData) -> Unit,
    onCancel: () -> Unit
) {
    var projectKey by remember { mutableStateOf("00000000000000000000000000000000") }
    var credentialId by remember { mutableStateOf("1234") }
    var keyError by remember { mutableStateOf("") }

    val canSave = projectKey.isNotBlank() &&
                  credentialId.isNotBlank() &&
                  isValidHex(projectKey)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Credential") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = projectKey,
                onValueChange = { newValue ->
                    projectKey = newValue.uppercase()
                    keyError = when {
                        newValue.isEmpty() -> ""
                        newValue.length != 32 -> "Key must be exactly 32 hex characters"
                        !isValidHex(newValue) -> "Key must contain only hex characters (0-9, A-F)"
                        else -> ""
                    }
                },
                label = { Text("Project Key (32 hex characters)") },
                isError = keyError.isNotEmpty(),
                supportingText = if (keyError.isNotEmpty()) {
                    { Text(keyError, color = MaterialTheme.colorScheme.error) }
                } else {
                    { Text("Enter 16-byte key as 32 hexadecimal characters") }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = credentialId,
                onValueChange = { credentialId = it },
                label = { Text("Credential ID") },
                supportingText = { Text("ASCII string identifier for this credential") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        try {
                            projectKey.hexToByteArray()
                            onSave(CredentialData(projectKey, credentialId))
                        } catch (e: Exception) {
                            keyError = e.message ?: "Invalid hex format"
                        }
                    },
                    enabled = canSave,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }
        }
    }
}