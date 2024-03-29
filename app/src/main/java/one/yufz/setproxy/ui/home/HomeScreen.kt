@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)

package one.yufz.setproxy.ui.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material3.Surface
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
                    items(proxyList) { item ->
                        ProxyCard(
                            proxy = item,
                            isChecked = item == current,
                            isActivated = item == current && isActivated,
                            onClick = {
                                switchActivation(item)
                            },
                            onDelete = {
                                viewModel.removeProxy(item)
                            },
                            onEdit = {
                                viewModel.requestEditProxy(item)
                            },
                            onCopy = {
                                viewModel.copyProxy(item)
                            }
                        )
                    }
                    item {
                        AddProxyCard {
                            viewModel.requestAddProxy()
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

            //Add proxy dialog
            if (uiState.requestAddProxy) {
                EditProxyDialog(
                    onDismissRequest = { viewModel.cancelAddProxy() },
                    dialogTitle = stringResource(id = R.string.add_proxy_dialog_title)
                ) { proxy ->
                    viewModel.addProxy(proxy)
                }
            }

            //Edit proxy dialog
            val editProxy = uiState.requestEditProxy
            if (editProxy != null) {
                EditProxyDialog(
                    initProxy = editProxy,
                    dialogTitle = stringResource(id = R.string.edit_proxy_dialog_title),
                    onDismissRequest = { viewModel.cancelEditProxy() },
                ) { proxy ->
                    viewModel.replaceProxy(editProxy, proxy)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProxyCard(proxy: Proxy, isChecked: Boolean, isActivated: Boolean, onClick: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit, onCopy: () -> Unit) {
    var showPopupMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(CardDefaults.shape)
            .combinedClickable(onClick = onClick, onLongClick = { showPopupMenu = true }),
    ) {
        CompositionLocalProvider(LocalContentColor provides if (isActivated) MaterialTheme.colorScheme.primary else LocalContentColor.current) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 64.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (proxy.name.isNotBlank()) {
                        Text(
                            text = proxy.name,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = "${proxy.host}:${proxy.port}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                AnimatedVisibility(isChecked) {
                    Icon(
                        imageVector = if (isActivated) Icons.Filled.Check else Icons.Outlined.Remove,
                        contentDescription = "checked",
                    )
                }
            }
        }
        if (showPopupMenu) {
            Surface(modifier = Modifier.align(Alignment.End)) {
                CardPopupMenu({ showPopupMenu = false }, onEdit, onDelete, onCopy)
            }
        }
    }
}

@Composable
private fun CardPopupMenu(onDismissRequest: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onCopy: () -> Unit) {
    DropdownMenu(expanded = true, onDismissRequest) {
        DropdownMenuItem(
            text = {
                Text(text = stringResource(R.string.edit_proxy))
            },
            onClick = {
                onEdit()
                onDismissRequest()
            }
        )

        DropdownMenuItem(
            text = {
                Text(text = stringResource(R.string.copy_proxy))
            }, onClick = {
                onCopy()
                onDismissRequest()
            }
        )

        DropdownMenuItem(
            text = {
                Text(text = stringResource(R.string.delete_proxy), color = MaterialTheme.colorScheme.error)
            }, onClick = {
                onDelete()
                onDismissRequest()
            }
        )
    }
}

@Composable
fun AddProxyCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .height(64.dp),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "add")
        }
    }
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
fun EditProxyDialog(
    initProxy: Proxy? = null,
    dialogTitle: String,
    onDismissRequest: () -> Unit,
    onConfirm: (proxy: Proxy) -> Unit
) {
    val nameFieldValue = remember { mutableStateOf(initProxy?.name ?: "") }
    val hostFieldValue = remember { mutableStateOf(initProxy?.host ?: "") }
    val portFieldValue = remember { mutableStateOf(initProxy?.port?.toString() ?: "") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = dialogTitle) },
        text = {
            Column {
                OutlinedTextField(
                    value = nameFieldValue.value,
                    singleLine = true,
                    label = { Text(text = stringResource(R.string.edit_proxy_dialog_input_name_hint)) },
                    onValueChange = { nameFieldValue.value = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = hostFieldValue.value,
                        singleLine = true,
                        label = { Text(text = stringResource(R.string.edit_proxy_dialog_input_host_hint)) },
                        onValueChange = { hostFieldValue.value = it },
                        modifier = Modifier.weight(2f)
                    )
                    Text(text = ":", modifier = Modifier.padding(8.dp))
                    OutlinedTextField(
                        value = portFieldValue.value,
                        singleLine = true,
                        label = { Text(text = stringResource(R.string.edit_proxy_dialog_input_port_hint)) },
                        onValueChange = { portFieldValue.value = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val text = hostFieldValue.value + ":" + portFieldValue.value
                    if (Proxy.isValidProxy(text)) {
                        onDismissRequest()
                        onConfirm(Proxy.fromAddress(text).copy(name = nameFieldValue.value))
                    } else {
                        Toast.makeText(context, "Invalid proxy format", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text(text = stringResource(R.string.edit_proxy_dialog_ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text(text = stringResource(R.string.edit_proxy_dialog_button_cancel))
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