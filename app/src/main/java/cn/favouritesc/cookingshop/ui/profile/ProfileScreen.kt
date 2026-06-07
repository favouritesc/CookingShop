package cn.favouritesc.cookingshop.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.favouritesc.cookingshop.sync.ClientState
import cn.favouritesc.cookingshop.sync.DiscoveredHost
import cn.favouritesc.cookingshop.sync.SyncManager
import cn.favouritesc.cookingshop.sync.SyncRole
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import cn.favouritesc.cookingshop.ui.components.CommonTopBar
import cn.favouritesc.cookingshop.ui.theme.Primary

private val LightGray = Color(0xFFF5F5F5)
private val IconColors = listOf(
    Color(0xFFFF8F00), Color(0xFF4CAF50), Color(0xFF2196F3),
    Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF607D8B)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onDishLibraryClick: () -> Unit,
    onIngredientListClick: () -> Unit,
    onOrderHistoryClick: () -> Unit,
    onIngredientSummaryClick: () -> Unit,
    onAboutClick: () -> Unit,
    syncManager: SyncManager? = null
) {
    val syncStatus by syncManager?.status?.collectAsState() ?: remember { mutableStateOf(null) }
    val discoveredHosts by syncManager?.nsdHelper?.discoveredHosts?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val isDiscovering by syncManager?.nsdHelper?.isDiscovering?.collectAsState() ?: remember { mutableStateOf(false) }
    var showRoleDialog by remember { mutableStateOf(false) }
    var showDiscoveryDialog by remember { mutableStateOf(false) }
    var showManualJoinDialog by remember { mutableStateOf(false) }
    var joinIpText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val startDiscoveryAction = { syncManager?.nsdHelper?.startDiscovery() }
    val nsdPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startDiscoveryAction()
    }
    val requestNsdPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            nsdPermissionLauncher.launch(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            startDiscoveryAction()
        }
    }

    Scaffold(
        topBar = { CommonTopBar(title = "我的") }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(LightGray)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Hero 头像区
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(72.dp).clip(CircleShape).background(Primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(52.dp),
                            tint = Primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "CookingShop", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "智能点餐 · 轻松烹饪", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 菜单区
            ProfileMenuGroup(
                items = listOf(
                    ProfileMenuItem("菜品管理", "浏览和编辑菜品库", Icons.AutoMirrored.Filled.MenuBook, IconColors[0], onDishLibraryClick),
                    ProfileMenuItem("备菜管理", "管理食材分类和清单", Icons.Default.Inventory2, IconColors[1], onIngredientListClick),
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            ProfileMenuGroup(
                items = listOf(
                    ProfileMenuItem("订单历史", "查看历史点餐记录", Icons.AutoMirrored.Filled.ReceiptLong, IconColors[2], onOrderHistoryClick),
                    ProfileMenuItem("备菜汇总", "查看备菜需求统计", Icons.Default.Assessment, IconColors[4], onIngredientSummaryClick),
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            ProfileMenuGroup(
                items = listOf(
                    ProfileMenuItem("关于", "版本 1.0.0", Icons.Default.Info, IconColors[5], onAboutClick),
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 局域网同步
            if (syncManager != null) {
                when (syncStatus?.role) {
                    SyncRole.HOST -> SyncHostPanel(
                        syncManager = syncManager,
                        onStop = { syncManager.stopHost() }
                    )
                    SyncRole.CLIENT -> SyncClientPanel(
                        syncManager = syncManager,
                        syncStatus = syncStatus,
                        onDisconnect = { coroutineScope.launch { syncManager.leaveHost() } }
                    )
                    else -> SyncMenuEntry(
                        onClick = { showRoleDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 角色选择弹窗
            if (showRoleDialog) {
                RoleSelectDialog(
                    onHost = {
                        showRoleDialog = false
                        syncManager?.startHost()
                    },
                    onJoin = {
                        showRoleDialog = false
                        showDiscoveryDialog = true
                        requestNsdPermission()
                    },
                    onDismiss = { showRoleDialog = false }
                )
            }

            // 加入设备 — 扫描弹窗
            if (showDiscoveryDialog) {
                DiscoverHostsDialog(
                    hosts = discoveredHosts,
                    isDiscovering = isDiscovering,
                    onScan = { requestNsdPermission() },
                    onConnect = { host ->
                        coroutineScope.launch { syncManager?.joinHost(host.ip, host.port) }
                        showDiscoveryDialog = false
                        syncManager?.nsdHelper?.stopDiscovery()
                    },
                    onManualInput = {
                        showDiscoveryDialog = false
                        showManualJoinDialog = true
                        syncManager?.nsdHelper?.stopDiscovery()
                    },
                    onDismiss = {
                        showDiscoveryDialog = false
                        syncManager?.nsdHelper?.stopDiscovery()
                    }
                )
            }

            // 手动输入IP弹窗（回退方案）
            if (showManualJoinDialog) {
                JoinInputDialog(
                    ipText = joinIpText,
                    onIpChange = { joinIpText = it },
                    onConnect = {
                        if (joinIpText.isNotBlank()) {
                            val input = joinIpText.trim()
                            val parts = input.split(":")
                            val ip = parts[0]
                            val port = parts.getOrNull(1)?.toIntOrNull() ?: 8765
                            coroutineScope.launch { syncManager?.joinHost(ip, port) }
                            showManualJoinDialog = false
                            joinIpText = ""
                        }
                    },
                    onDismiss = {
                        showManualJoinDialog = false
                        joinIpText = ""
                    }
                )
            }
        }
    }
}

data class ProfileMenuItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color,
    val onClick: () -> Unit
)

@Composable
private fun ProfileMenuGroup(items: List<ProfileMenuItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { item.onClick() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 图标（彩色底）
                    Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(item.iconColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = item.icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = item.iconColor)
                    }
                    // 文字
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    // 箭头
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFFBDBDBD)
                    )
                }
                if (index < items.size - 1) {
                    Box(modifier = Modifier.fillMaxWidth().padding(start = 70.dp)) {
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFFEEEEEE)))
                    }
                }
            }
        }
    }
}

// ========== 局域网同步组件 ==========

/** 未连接状态：点击进入角色选择 */
@Composable
private fun SyncMenuEntry(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(22.dp), tint = Primary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "局域网同步", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "多设备数据实时同步", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFFBDBDBD)
            )
        }
    }
}

/** 主机模式面板 */
@Composable
private fun SyncHostPanel(syncManager: SyncManager, onStop: () -> Unit) {
    val localIp = remember { syncManager.getLocalIpAddress() }
    val hostPort by syncManager.hostPort.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF4CAF50))
                )
                Text(text = "主机模式运行中", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = Color(0xFF4CAF50))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "$localIp:$hostPort",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            Text(
                text = "其他设备输入此地址即可连接",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
            ) {
                Text("停止主机", color = Color.White)
            }
        }
    }
}

/** 客户端连接面板 */
@Composable
private fun SyncClientPanel(syncManager: SyncManager, syncStatus: cn.favouritesc.cookingshop.sync.SyncStatus?, onDisconnect: () -> Unit) {
    val hostIp by syncManager.hostIp.collectAsState()
    val hostPort by syncManager.hostPort.collectAsState()
    val clientState = syncStatus?.clientState ?: ClientState.DISCONNECTED

    val (statusText, statusColor) = when (clientState) {
        ClientState.CONNECTED -> "已连接" to Color(0xFF4CAF50)
        ClientState.CONNECTING -> "连接中…" to Primary
        ClientState.DISCONNECTED -> "已断开" to Color(0xFFE53935)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.size(12.dp).clip(CircleShape).background(statusColor)
                )
                Text(text = statusText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = statusColor)
            }
            if (clientState == ClientState.CONNECTED) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "$hostIp:$hostPort",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                Text(
                    text = "已连接到主机",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (clientState == ClientState.CONNECTING) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "正在连接到 $hostIp:$hostPort...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onDisconnect,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
            ) {
                Text("断开连接", color = Color.White)
            }
        }
    }
}

/** 角色选择弹窗 */
@Composable
private fun RoleSelectDialog(onHost: () -> Unit, onJoin: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("局域网同步", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("选择本设备的角色", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onHost,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("开启主机", color = Color.White)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onJoin,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Text("加入其他设备", color = Color.White)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/** 自动发现主机弹窗：显示扫描结果列表 + 手动输入回退 */
@Composable
private fun DiscoverHostsDialog(
    hosts: List<DiscoveredHost>,
    isDiscovering: Boolean,
    onScan: () -> Unit,
    onConnect: (DiscoveredHost) -> Unit,
    onManualInput: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("发现主机", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "扫描局域网中的 CookingShop 主机",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onScan,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isDiscovering,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text(
                        if (isDiscovering) "扫描中…" else "重新扫描",
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (hosts.isEmpty() && !isDiscovering) {
                    Text(
                        "未发现主机，请确认主机已开启且在同一 WiFi 下",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF999999)
                    )
                } else if (hosts.isNotEmpty()) {
                    hosts.forEach { host ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onConnect(host) },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = host.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${host.ip}:${host.port}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFFBDBDBD)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onManualInput) {
                Text("手动输入地址", color = Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/** IP 地址输入弹窗 */
@Composable
private fun JoinInputDialog(ipText: String, onIpChange: (String) -> Unit, onConnect: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("加入主机", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("输入主机的 IP 地址和端口", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = ipText,
                    onValueChange = onIpChange,
                    label = { Text("主机地址") },
                    placeholder = { Text("例如 192.168.1.100:8765") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConnect,
                enabled = ipText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("连接", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}


