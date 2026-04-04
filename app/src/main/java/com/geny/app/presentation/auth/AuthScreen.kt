package com.geny.app.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.geny.app.core.ui.components.LoadingOverlay
import com.geny.app.domain.model.AuthState

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthenticated: () -> Unit,
    onNeedServerSetup: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigate on successful auth
    LaunchedEffect(uiState.authState) {
        if (uiState.authState is AuthState.Authenticated) {
            onAuthenticated()
        }
    }

    // Show errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        // Show loading during auto-login attempt
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Connecting...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Text(
                text = "geny",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Autonomous Agent Orchestration",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Connection form
            ConnectionForm(
                serverUrl = uiState.serverUrl,
                username = uiState.username,
                password = uiState.password,
                displayName = uiState.displayName,
                autoLogin = uiState.autoLogin,
                isSubmitting = uiState.isSubmitting,
                connectionStatus = uiState.connectionStatus,
                isSetupMode = uiState.authState is AuthState.NoUsers,
                onServerUrlChanged = viewModel::onServerUrlChanged,
                onUsernameChanged = viewModel::onUsernameChanged,
                onPasswordChanged = viewModel::onPasswordChanged,
                onDisplayNameChanged = viewModel::onDisplayNameChanged,
                onAutoLoginChanged = viewModel::onAutoLoginChanged,
                onCheckServer = viewModel::checkServer,
                onConnect = viewModel::connect
            )
        }
    }
}

@Composable
private fun ConnectionForm(
    serverUrl: String,
    username: String,
    password: String,
    displayName: String,
    autoLogin: Boolean,
    isSubmitting: Boolean,
    connectionStatus: ConnectionStatus,
    isSetupMode: Boolean,
    onServerUrlChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    onAutoLoginChanged: (Boolean) -> Unit,
    onCheckServer: () -> Unit,
    onConnect: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }

    // Connection status indicator color
    val statusColor by animateColorAsState(
        when (connectionStatus) {
            ConnectionStatus.REACHABLE -> MaterialTheme.colorScheme.primary
            ConnectionStatus.UNREACHABLE -> MaterialTheme.colorScheme.error
            ConnectionStatus.CHECKING -> MaterialTheme.colorScheme.tertiary
            ConnectionStatus.IDLE -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "statusColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- Section: Server ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Dns,
                    null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Server",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            OutlinedTextField(
                value = serverUrl,
                onValueChange = onServerUrlChanged,
                label = { Text("Server URL") },
                placeholder = { Text("http://192.168.1.100:8000") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    when (connectionStatus) {
                        ConnectionStatus.CHECKING -> CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        ConnectionStatus.REACHABLE -> Icon(
                            Icons.Filled.CheckCircle,
                            "Connected",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        ConnectionStatus.UNREACHABLE -> Icon(
                            Icons.Filled.CloudOff,
                            "Unreachable",
                            tint = MaterialTheme.colorScheme.error
                        )
                        ConnectionStatus.IDLE -> Icon(
                            Icons.Filled.Cloud,
                            "Not tested",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = {
                        onCheckServer()
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                ),
                enabled = !isSubmitting
            )

            OutlinedButton(
                onClick = onCheckServer,
                modifier = Modifier.fillMaxWidth(),
                enabled = serverUrl.isNotBlank() && !isSubmitting && connectionStatus != ConnectionStatus.CHECKING
            ) {
                Text("Test Connection")
            }

            // Show setup mode indicator
            AnimatedVisibility(visible = isSetupMode) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Text(
                        text = "No users found. Create the first admin account.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // --- Section: Credentials ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Cloud,
                    null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSetupMode) "Create Account" else "Credentials",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChanged,
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                enabled = !isSubmitting
            )

            // Display name field (setup mode only)
            AnimatedVisibility(visible = isSetupMode) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = onDisplayNameChanged,
                    label = { Text("Display Name (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    enabled = !isSubmitting
                )
            }

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChanged,
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onConnect() }),
                enabled = !isSubmitting
            )

            Spacer(modifier = Modifier.height(4.dp))

            // --- Auto Login Toggle ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto Login",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Save credentials and connect automatically",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoLogin,
                    onCheckedChange = onAutoLoginChanged,
                    enabled = !isSubmitting
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Connect Button ---
            Button(
                onClick = onConnect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = serverUrl.isNotBlank()
                        && username.isNotBlank()
                        && password.isNotBlank()
                        && !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Connecting...")
                } else {
                    Text(
                        text = if (isSetupMode) "Create & Connect" else "Connect",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    }
}
