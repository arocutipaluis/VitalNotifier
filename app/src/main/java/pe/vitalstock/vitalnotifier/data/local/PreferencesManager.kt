package pe.vitalstock.vitalnotifier.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import pe.vitalstock.vitalnotifier.data.model.ServerConnection

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val gson = Gson()

    var isForwardingEnabled: Boolean
        get() = prefs.getBoolean(KEY_FORWARDING_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_FORWARDING_ENABLED, value).apply()

    var isAutoRetryEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_RETRY, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_RETRY, value).apply()

    fun getConnections(): List<ServerConnection> {
        val json = prefs.getString(KEY_CONNECTIONS, null)
        if (json.isNullOrBlank()) {
            val legacyIp = prefs.getString(KEY_SERVER_IP, DEFAULT_IP) ?: DEFAULT_IP
            val legacyPort = prefs.getInt(KEY_SERVER_PORT, DEFAULT_PORT)
            val defaultList = listOf(
                ServerConnection(
                    id = "default_pc",
                    name = "Caja Principal",
                    ip = legacyIp,
                    port = legacyPort,
                    isEnabled = true
                )
            )
            saveConnections(defaultList)
            return defaultList
        }

        return try {
            val type = object : TypeToken<List<ServerConnection>>() {}.type
            val list: List<ServerConnection> = gson.fromJson(json, type) ?: emptyList()
            if (list.isEmpty()) {
                val defaultList = listOf(
                    ServerConnection(
                        id = "default_pc",
                        name = "Caja Principal",
                        ip = DEFAULT_IP,
                        port = DEFAULT_PORT,
                        isEnabled = true
                    )
                )
                saveConnections(defaultList)
                defaultList
            } else {
                list
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveConnections(connections: List<ServerConnection>) {
        val json = gson.toJson(connections)
        prefs.edit().putString(KEY_CONNECTIONS, json).apply()
    }

    fun addConnection(conn: ServerConnection) {
        val current = getConnections().toMutableList()
        current.add(conn)
        saveConnections(current)
    }

    fun updateConnection(conn: ServerConnection) {
        val current = getConnections().toMutableList()
        val index = current.indexOfFirst { it.id == conn.id }
        if (index != -1) {
            current[index] = conn
            saveConnections(current)
        }
    }

    fun deleteConnection(id: String) {
        val current = getConnections().toMutableList()
        current.removeAll { it.id == id }
        saveConnections(current)
    }

    fun toggleConnection(id: String, enabled: Boolean) {
        val current = getConnections().toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            current[index] = current[index].copy(isEnabled = enabled)
            saveConnections(current)
        }
    }

    // Compatibilidad legada
    var serverIp: String
        get() = getConnections().firstOrNull()?.ip ?: DEFAULT_IP
        set(value) {
            val list = getConnections().toMutableList()
            if (list.isNotEmpty()) {
                list[0] = list[0].copy(ip = value.trim())
                saveConnections(list)
            }
        }

    var serverPort: Int
        get() = getConnections().firstOrNull()?.port ?: DEFAULT_PORT
        set(value) {
            val list = getConnections().toMutableList()
            if (list.isNotEmpty()) {
                list[0] = list[0].copy(port = value)
                saveConnections(list)
            }
        }

    val baseUrl: String
        get() = getConnections().firstOrNull()?.baseUrl ?: "http://${DEFAULT_IP}:${DEFAULT_PORT}"

    companion object {
        private const val PREFS_NAME = "vitalnotifier_prefs"
        private const val KEY_CONNECTIONS = "server_connections_list"
        private const val KEY_SERVER_IP = "server_ip"
        private const val KEY_SERVER_PORT = "server_port"
        private const val KEY_FORWARDING_ENABLED = "forwarding_enabled"
        private const val KEY_AUTO_RETRY = "auto_retry"

        const val DEFAULT_IP = "192.168.100.36"
        const val DEFAULT_PORT = 5005
    }
}
