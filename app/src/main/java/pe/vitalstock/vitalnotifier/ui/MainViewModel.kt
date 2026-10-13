package pe.vitalstock.vitalnotifier.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.vitalstock.vitalnotifier.VitalNotifierApp
import pe.vitalstock.vitalnotifier.data.model.PaymentEntity
import pe.vitalstock.vitalnotifier.data.model.ServerConnection
import pe.vitalstock.vitalnotifier.data.model.YapePayment
import pe.vitalstock.vitalnotifier.util.NotificationPermissionHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class PingUiState {
    object Idle : PingUiState()
    object Loading : PingUiState()
    data class Success(val message: String) : PingUiState()
    data class Error(val error: String) : PingUiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VitalNotifierApp
    private val prefs = app.preferencesManager
    private val dispatcher = app.paymentDispatcher
    private val dao = app.database.paymentDao()

    // Lista de conexiones a múltiples PCs
    val connections = MutableStateFlow<List<ServerConnection>>(prefs.getConnections())

    val isForwardingEnabled = MutableStateFlow(prefs.isForwardingEnabled)

    // Estado del permiso del sistema
    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    private val _isBatteryOptIgnored = MutableStateFlow(true)
    val isBatteryOptIgnored: StateFlow<Boolean> = _isBatteryOptIgnored.asStateFlow()

    // Estados de Ping por cada PC (mapa con ID de la conexión -> PingUiState)
    private val _pingStates = MutableStateFlow<Map<String, PingUiState>>(emptyMap())
    val pingStates: StateFlow<Map<String, PingUiState>> = _pingStates.asStateFlow()

    // Historial reactivo de pagos desde SQLite (Room)
    val payments: StateFlow<List<PaymentEntity>> = dao.getAllPaymentsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshPermissionStatus()
        refreshConnections()
    }

    fun refreshPermissionStatus() {
        _isPermissionGranted.value =
            NotificationPermissionHelper.isNotificationAccessGranted(getApplication())
        _isBatteryOptIgnored.value =
            NotificationPermissionHelper.isBatteryOptimizationIgnored(getApplication())
    }

    fun refreshConnections() {
        connections.value = prefs.getConnections()
    }

    fun addConnection(name: String, ip: String, port: Int) {
        val cleanName = name.ifBlank { "PC ${connections.value.size + 1}" }
        val cleanIp = ip.trim().removeSuffix("/")
        val newConn = ServerConnection(
            name = cleanName,
            ip = cleanIp,
            port = if (port > 0) port else 5005,
            isEnabled = true
        )
        prefs.addConnection(newConn)
        refreshConnections()
    }

    fun updateConnection(id: String, name: String, ip: String, port: Int, isEnabled: Boolean) {
        val cleanName = name.ifBlank { "PC" }
        val cleanIp = ip.trim().removeSuffix("/")
        val updated = ServerConnection(
            id = id,
            name = cleanName,
            ip = cleanIp,
            port = if (port > 0) port else 5005,
            isEnabled = isEnabled
        )
        prefs.updateConnection(updated)
        refreshConnections()
    }

    fun deleteConnection(id: String) {
        prefs.deleteConnection(id)
        refreshConnections()
        // Limpiar estado de ping de esa PC
        _pingStates.value = _pingStates.value.toMutableMap().apply { remove(id) }
    }

    fun toggleConnection(id: String, isEnabled: Boolean) {
        prefs.toggleConnection(id, isEnabled)
        refreshConnections()
    }

    fun toggleForwarding(enabled: Boolean) {
        isForwardingEnabled.value = enabled
        prefs.isForwardingEnabled = enabled
    }

    fun testConnection(conn: ServerConnection) {
        viewModelScope.launch {
            _pingStates.value = _pingStates.value.toMutableMap().apply {
                put(conn.id, PingUiState.Loading)
            }
            val result = dispatcher.ping(conn.baseUrl)
            _pingStates.value = _pingStates.value.toMutableMap().apply {
                if (result.isSuccess) {
                    put(conn.id, PingUiState.Success(result.getOrDefault("Conectado (HTTP 200)")))
                } else {
                    val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Error de conexión"
                    put(conn.id, PingUiState.Error(errorMsg))
                }
            }
        }
    }

    fun resetPingState(id: String) {
        _pingStates.value = _pingStates.value.toMutableMap().apply {
            put(id, PingUiState.Idle)
        }
    }

    fun retryPayment(paymentId: Long) {
        viewModelScope.launch {
            dispatcher.retryPayment(paymentId)
        }
    }

    fun retryAllFailed() {
        viewModelScope.launch {
            dispatcher.retryAllFailed()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            dao.clearAll()
        }
    }
}
