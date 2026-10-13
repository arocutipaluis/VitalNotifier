package pe.vitalstock.vitalnotifier.data.model

import java.util.UUID

data class ServerConnection(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val ip: String,
    val port: Int = 5005,
    val isEnabled: Boolean = true
) {
    val baseUrl: String
        get() = "http://${ip.trim().removeSuffix("/")}:${port}"

    val yapeEndpoint: String
        get() = "${baseUrl}/yape"

    val pingEndpoint: String
        get() = "${baseUrl}/ping"
}
