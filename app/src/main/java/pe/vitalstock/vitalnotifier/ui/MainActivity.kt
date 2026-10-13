package pe.vitalstock.vitalnotifier.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import pe.vitalstock.vitalnotifier.data.model.ServerConnection
import pe.vitalstock.vitalnotifier.ui.components.EditConnectionDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.vitalstock.vitalnotifier.data.model.DispatchStatus
import pe.vitalstock.vitalnotifier.ui.components.PaymentItemCard
import pe.vitalstock.vitalnotifier.ui.theme.ErrorRed
import pe.vitalstock.vitalnotifier.ui.theme.ErrorRedBg
import pe.vitalstock.vitalnotifier.ui.theme.SuccessGreen
import pe.vitalstock.vitalnotifier.ui.theme.SuccessGreenBg
import pe.vitalstock.vitalnotifier.ui.theme.TealPrimary
import pe.vitalstock.vitalnotifier.ui.theme.VitalNotifierTheme
import pe.vitalstock.vitalnotifier.ui.theme.WarningOrange
import pe.vitalstock.vitalnotifier.ui.theme.WarningOrangeBg
import pe.vitalstock.vitalnotifier.util.NotificationPermissionHelper

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VitalNotifierTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Al regresar de los Ajustes de Android, re-evaluar si se otorgó el permiso
        viewModel.refreshPermissionStatus()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val isPermissionGranted by viewModel.isPermissionGranted.collectAsState()
    val isBatteryOptIgnored by viewModel.isBatteryOptIgnored.collectAsState()
    val isForwardingEnabled by viewModel.isForwardingEnabled.collectAsState()
    val connections by viewModel.connections.collectAsState()
    val pingStates by viewModel.pingStates.collectAsState()
    val payments by viewModel.payments.collectAsState()

    var showAddConnectionDialog by remember { mutableStateOf(false) }
    var editingConnection by remember { mutableStateOf<ServerConnection?>(null) }

    val hasFailedPayments = payments.any { it.status == DispatchStatus.FAILED }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "VitalNotifier",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Enlace Yape ↔ VitalStock Desktop",
                            fontSize = 12.sp,
                            color = Color(0xFFCCFBF1)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TealPrimary),
                actions = {
                    if (payments.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearHistory() }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Limpiar historial",
                                tint = Color.White
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF1F5F9)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Tarjeta de Estado del Permiso de Notificaciones
            item {
                PermissionCard(
                    isGranted = isPermissionGranted,
                    onOpenSettings = {
                        val intent = NotificationPermissionHelper.createNotificationAccessSettingsIntent(context)
                        context.startActivity(intent)
                    }
                )
            }

            // 1.1 Tarjeta de Desactivación de Ahorro de Batería (si aplica)
            if (!isBatteryOptIgnored) {
                item {
                    BatteryOptimizationCard(
                        onOpenBatterySettings = {
                            val intent = NotificationPermissionHelper.createIgnoreBatteryOptimizationIntent(context)
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // 2. Control de Reenvío Automático
            item {
                ForwardingSwitchCard(
                    isEnabled = isForwardingEnabled,
                    onToggle = { viewModel.toggleForwarding(it) }
                )
            }

            // 3. Tarjeta de Conexiones a múltiples PCs de VitalStock
            item {
                MultipleServersConfigCard(
                    connections = connections,
                    pingStates = pingStates,
                    onAddClick = { showAddConnectionDialog = true },
                    onEditClick = { editingConnection = it },
                    onDeleteClick = { viewModel.deleteConnection(it.id) },
                    onToggle = { conn, enabled -> viewModel.toggleConnection(conn.id, enabled) },
                    onTestConnection = { viewModel.testConnection(it) }
                )
            }

            // 4. Reintentar pagos fallidos si existen
            if (hasFailedPayments) {
                item {
                    OutlinedButton(
                        onClick = { viewModel.retryAllFailed() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = WarningOrange
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reintentar Pagos Fallidos", fontSize = 13.sp, color = WarningOrange)
                    }
                }
            }

            // 5. Encabezado de la Lista de Pagos
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Historial de Pagos Yape",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                    Text(
                        text = "${payments.size} capturados",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // 6. Lista de Pagos Capturados o Estado Vacío
            if (payments.isEmpty()) {
                item {
                    EmptyPaymentsState()
                }
            } else {
                items(payments, key = { it.id }) { item ->
                    PaymentItemCard(
                        payment = item,
                        onRetry = { viewModel.retryPayment(item.id) }
                    )
                }
            }
        }
    }

    if (showAddConnectionDialog) {
        EditConnectionDialog(
            initialConnection = null,
            onDismiss = { showAddConnectionDialog = false },
            onSave = { name, ip, port ->
                viewModel.addConnection(name, ip, port)
                Toast.makeText(context, "PC agregada: $name", Toast.LENGTH_SHORT).show()
            }
        )
    }

    editingConnection?.let { conn ->
        EditConnectionDialog(
            initialConnection = conn,
            onDismiss = { editingConnection = null },
            onSave = { name, ip, port ->
                viewModel.updateConnection(conn.id, name, ip, port, conn.isEnabled)
                Toast.makeText(context, "PC actualizada: $name", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun PermissionCard(
    isGranted: Boolean,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) SuccessGreenBg else ErrorRedBg
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                contentDescription = null,
                tint = if (isGranted) SuccessGreen else ErrorRed,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isGranted) "Acceso a Notificaciones Activo" else "Permiso de Notificaciones Requerido",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isGranted) Color(0xFF14532D) else Color(0xFF7F1D1D)
                )
                Text(
                    text = if (isGranted)
                        "La app está autorizada para escuchar pagos de Yape en tiempo real."
                    else
                        "Debes activar el Acceso a Notificaciones para que la app capture pagos.",
                    fontSize = 12.sp,
                    color = if (isGranted) Color(0xFF166534) else Color(0xFF991B1B)
                )
            }

            if (!isGranted) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onOpenSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("Activar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BatteryOptimizationCard(
    onOpenBatterySettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = WarningOrangeBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = WarningOrange,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ahorro de Batería Detectado",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF7C2D12)
                )
                Text(
                    text = "Excluye a VitalNotifier del ahorro de batería para que Android no duerma el servicio entre pagos de Yape.",
                    fontSize = 11.sp,
                    color = Color(0xFF9A3412)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Button(
                onClick = onOpenBatterySettings,
                colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Permitir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ForwardingSwitchCard(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Reenvío Automático a VitalStock",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = if (isEnabled) "Reenviando pagos por red local" else "Reenvío en pausa",
                    fontSize = 12.sp,
                    color = if (isEnabled) SuccessGreen else Color(0xFF64748B)
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = TealPrimary
                )
            )
        }
    }
}

@Composable
fun MultipleServersConfigCard(
    connections: List<ServerConnection>,
    pingStates: Map<String, PingUiState>,
    onAddClick: () -> Unit,
    onEditClick: (ServerConnection) -> Unit,
    onDeleteClick: (ServerConnection) -> Unit,
    onToggle: (ServerConnection, Boolean) -> Unit,
    onTestConnection: (ServerConnection) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "PCs Receptoras (VitalStock)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF1E293B)
                        )
                        val activeCount = connections.count { it.isEnabled }
                        Text(
                            text = if (connections.isEmpty()) "Sin computadoras configuradas"
                            else "$activeCount de ${connections.size} activas",
                            fontSize = 11.sp,
                            color = if (activeCount > 0) SuccessGreen else Color(0xFF64748B)
                        )
                    }
                }

                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Añadir PC", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (connections.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No hay PCs conectadas",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Presiona '+ Añadir PC' para ingresar la IP de cada computadora con VitalStock.",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    connections.forEach { conn ->
                        val ping = pingStates[conn.id] ?: PingUiState.Idle
                        ServerConnectionItem(
                            connection = conn,
                            pingState = ping,
                            onToggle = { onToggle(conn, it) },
                            onEdit = { onEditClick(conn) },
                            onDelete = { onDeleteClick(conn) },
                            onPing = { onTestConnection(conn) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ServerConnectionItem(
    connection: ServerConnection,
    pingState: PingUiState,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPing: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (connection.isEnabled) Color(0xFFF8FAFC) else Color(0xFFF1F5F9)
        ),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (connection.isEnabled) Color(0xFFE2E8F0) else Color(0xFFCBD5E1)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Computer,
                        contentDescription = null,
                        tint = if (connection.isEnabled) TealPrimary else Color(0xFF94A3B8),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = connection.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (connection.isEnabled) Color(0xFF0F172A) else Color(0xFF64748B)
                        )
                        Text(
                            text = "${connection.ip}:${connection.port}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (connection.isEnabled) Color(0xFF334155) else Color(0xFF94A3B8)
                        )
                    }
                }

                Switch(
                    checked = connection.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = TealPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Fila de acciones: Botón Ping, Editar, Eliminar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = onPing,
                    enabled = connection.isEnabled,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    if (pingState is PingUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp,
                            color = TealPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Probando...", fontSize = 11.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (connection.isEnabled) TealPrimary else Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Probar conexión",
                            fontSize = 11.sp,
                            color = if (connection.isEnabled) TealPrimary else Color(0xFF94A3B8)
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = ErrorRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Estado y feedback de Ping
            AnimatedVisibility(visible = pingState !is PingUiState.Idle && pingState !is PingUiState.Loading) {
                when (pingState) {
                    is PingUiState.Success -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(SuccessGreenBg)
                                .padding(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Conectado OK: ${pingState.message}",
                                    fontSize = 11.sp,
                                    color = SuccessGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    is PingUiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(ErrorRedBg)
                                .padding(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WifiOff,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sin conexión: ${pingState.error}",
                                    fontSize = 11.sp,
                                    color = ErrorRed,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }
    }
}

@Composable
fun EmptyPaymentsState() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Payment,
                contentDescription = null,
                tint = Color(0xFFCBD5E1),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Esperando notificaciones de Yape",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF475569)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Cuando un cliente transfiera dinero por Yape, la notificación aparecerá aquí y se enviará automáticamente a las PCs de VitalStock.",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
