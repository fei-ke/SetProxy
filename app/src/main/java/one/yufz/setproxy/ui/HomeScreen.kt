@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package one.yufz.setproxy.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import one.yufz.setproxy.Proxy

@Composable
fun HomeScreen() {
    Scaffold() { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            val viewModel: ProxyViewModel = viewModel()
            val current by viewModel.currentProxy.collectAsState(Proxy.EMPTY_PROXY)
            val proxyList by viewModel.proxyList.collectAsState(emptyList())

            Column {
                StatusCard(current)
                LazyColumn() {
                    items(proxyList) {
                        val isActivated = it == current
                        ProxyCard(it, isActivated) {
                            if (isActivated) {
                                viewModel.deactivateProxy()
                            } else {
                                viewModel.activateProxy(it)
                            }

                        }
                    }
                    item {
                        AddProxy {
                            viewModel.addProxy(it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProxyCard(proxy: Proxy, isActivated: Boolean, onActive: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .height(64.dp),
        onClick = onActive
    ) {
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
            if (isActivated) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = "activated")
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
        AddProxyDialog(onDismissRequest = { showAddDialog = false },
            onConfirm = { text ->
                showAddDialog = false
                val (host, port) = text.split(":")
                onAdd(Proxy(host, port.toInt()))
            }
        )
    }
}

@Composable
fun StatusCard(proxy: Proxy) {
    val empty = proxy.isEmpty()
    val colors = if (empty) CardDefaults.cardColors() else
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(100.dp),
        colors = colors,
        onClick = { }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (empty) {
                Text(
                    text = "No Proxy Set",
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
        title = { Text(text = "Add Proxy") },
        text = {
            OutlinedTextField(
                value = textFieldValue.value,
                singleLine = true,
                placeholder = { Text(text = "host:port") },
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
                Text(text = "Add")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text(text = "Cancel")
            }
        },
    )
}