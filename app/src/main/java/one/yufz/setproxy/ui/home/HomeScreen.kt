@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package one.yufz.setproxy.ui.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import one.yufz.setproxy.Permission
import one.yufz.setproxy.Proxy
import one.yufz.setproxy.R

@Composable
fun HomeScreen() {
    Scaffold() { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            val viewModel: ProxyViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()
            val isActivated = uiState.isActivated
            val current = uiState.currentProxy
            val proxyList = uiState.proxyList

            fun switchActivation(proxy: Proxy) {
                if (proxy == current && isActivated) {
                    viewModel.deactivateProxy()
                } else {
                    viewModel.setCurrentProxy(proxy, true)
                }
            }

            Column {
                StatusCard(current, isActivated) {
                    if (!current.isEmpty()) {
                        switchActivation(current)
                    }
                }
                LazyColumn() {
                    items(proxyList) {
                        ProxyCard(
                            proxy = it,
                            isChecked = it == current,
                            isActivated = it == current && isActivated,
                            onClick = {
                                switchActivation(it)
                            }, onDelete = {
                                viewModel.removeProxy(it)
                            }
                        )
                    }
                    item {
                        AddProxy {
                            viewModel.addProxy(it)
                        }
                    }
                }
            }

            if (uiState.requestingPermission) {
                RequestPermissionDialog(
                    onRoot = {
                        viewModel.requestPermissionUseRoot()
                    },
                    onDismissRequest = {
                        viewModel.cancelRequestPermission()
                    }
                )
            }
        }
    }
}

@Composable
fun ProxyCard(proxy: Proxy, isChecked: Boolean, isActivated: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    var showPopupMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .height(64.dp)
            .clip(CardDefaults.shape)
            .combinedClickable(onClick = onClick, onLongClick = { showPopupMenu = true }),
    ) {
        if (showPopupMenu) {
            DropdownMenu(expanded = true, onDismissRequest = { showPopupMenu = false }) {
                DropdownMenuItem(
                    text = {
                        Text(text = stringResource(R.string.delete_proxy))
                    }, onClick = {
                        onDelete()
                        showPopupMenu = false
                    }
                )
            }
        }
        CompositionLocalProvider(LocalContentColor provides if (isActivated) MaterialTheme.colorScheme.primary else LocalContentColor.current) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${proxy.host}:${proxy.port}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                AnimatedVisibility(isChecked) {
                    Icon(
                        imageVector = if (isActivated) Icons.Filled.Check else Icons.Outlined.Remove,
                        contentDescription = "checked",
                    )
                }
            }
        }
    }

}

@Composable
fun AddProxy(onAdd: (proxy: Proxy) -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .height(64.dp),
        onClick = {
            showAddDialog = true
        }) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "add")
        }
    }
    if (showAddDialog) {
        val context = LocalContext.current
        AddProxyDialog(onDismissRequest = { showAddDialog = false },
            onConfirm = { text ->
                if (checkProxyFormat(text)) {
                    showAddDialog = false
                    val (host, port) = text.split(":")
                    onAdd(Proxy(host, port.toInt()))
                } else {
                    Toast.makeText(context, "Invalid proxy format", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

fun checkProxyFormat(text: String): Boolean {
    val regex = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)|([\\w.-]+):(\\d+)\$".toRegex()
    return regex.matches(text)
}

@Composable
fun StatusCard(proxy: Proxy, isActivated: Boolean, onClick: () -> Unit) {
    val empty = proxy.isEmpty()
    val colors = if (isActivated) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    } else {
        CardDefaults.cardColors()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 8.dp)
            .height(100.dp),
        colors = colors,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (empty) {
                Text(
                    text = stringResource(R.string.proxy_not_set),
                    style = MaterialTheme.typography.displaySmall,
                )
            } else {
                Text(
                    text = "${proxy.host}:${proxy.port}",
                    style = MaterialTheme.typography.displaySmall,
                )
            }

        }
    }
}

@Composable
fun AddProxyDialog(onDismissRequest: () -> Unit, onConfirm: (text: String) -> Unit) {
    val textFieldValue = remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(R.string.add_proxy)) },
        text = {
            OutlinedTextField(
                value = textFieldValue.value,
                singleLine = true,
                placeholder = { Text(text = stringResource(R.string.add_proxy_input_hint)) },
                onValueChange = { textFieldValue.value = it },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(textFieldValue.value)
                }
            ) {
                Text(text = stringResource(R.string.add_proxy_dialog_ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text(text = stringResource(R.string.add_proxy_dialog_button_cancel))
            }
        },
    )
}

@Composable
fun RequestPermissionDialog(onRoot: () -> Unit, onDismissRequest: () -> Unit) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.permission_dialog_title)) },
        text = {
            SelectionContainer() {
                Text(
                    stringResource(id = R.string.permission_dialog_content, Permission.ADB_COMMAND),
                )
            }
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = {
                    onRoot()
                    onDismissRequest()
                }) {
                    Text(stringResource(R.string.permission_dialog_use_root))
                }
                Row {
                    TextButton(onClick = {
                        clipboardManager.setText(AnnotatedString(Permission.ADB_COMMAND))
                        onDismissRequest()
                    }) {
                        Text(stringResource(R.string.permission_dialog_copy_command))
                    }
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(R.string.permission_dialog_ok))
                    }
                }
            }

        }
    )
}