package pe.vitalstock.vitalnotifier.service

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import pe.vitalstock.vitalnotifier.data.local.PaymentDao
import pe.vitalstock.vitalnotifier.data.local.PreferencesManager
import pe.vitalstock.vitalnotifier.data.model.DispatchStatus
import pe.vitalstock.vitalnotifier.data.model.PaymentEntity
import pe.vitalstock.vitalnotifier.data.model.YapePayment
import pe.vitalstock.vitalnotifier.data.remote.VitalStockClient

class PaymentDispatcher(
    private val dao: PaymentDao,
    private val client: VitalStockClient,
    private val preferences: PreferencesManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Procesa y despacha un pago extraído de Yape hacia todas las PCs configuradas y activas.
     */
    fun processAndDispatch(payment: YapePayment, onComplete: ((Boolean) -> Unit)? = null) {
        scope.launch {
            // 1. Guardar en BD con estado inicial PENDING
            val initialEntity = PaymentEntity(
                remitente = payment.remitente,
                monto = payment.monto,
                fechaHora = payment.fechaHora,
                codigoSeg = payment.codigoSeg,
                mensajeRaw = payment.mensajeRaw,
                status = DispatchStatus.PENDING
            )
            val insertedId = dao.insertPayment(initialEntity)

            // Disparar alerta sonora y táctil fuerte y clara en el teléfono de la botica
            pe.vitalstock.vitalnotifier.VitalNotifierApp.instance.let { app ->
                pe.vitalstock.vitalnotifier.util.SoundAlertHelper.playPaymentReceivedAlert(app, payment)
            }

            // 2. Verificar si el reenvío está habilitado globalmente
            if (!preferences.isForwardingEnabled) {
                Log.w(TAG, "El reenvío automático está pausado por el usuario.")
                val updated = initialEntity.copy(
                    id = insertedId,
                    status = DispatchStatus.FAILED,
                    errorMessage = "Reenvío pausado en la configuración de la app"
                )
                dao.updatePayment(updated)
                onComplete?.invoke(false)
                return@launch
            }

            // 3. Obtener PCs activas
            val activeConnections = preferences.getConnections().filter { it.isEnabled }
            if (activeConnections.isEmpty()) {
                Log.w(TAG, "No hay PCs activas configuradas para recibir el pago.")
                val updated = initialEntity.copy(
                    id = insertedId,
                    status = DispatchStatus.FAILED,
                    errorMessage = "No hay PCs activas en la lista de conexiones"
                )
                dao.updatePayment(updated)
                onComplete?.invoke(false)
                return@launch
            }

            // 4. Despachar concurrentemente a todas las PCs activas
            val dispatchResults = activeConnections.map { conn ->
                async {
                    val result = client.sendPayment(conn.baseUrl, payment)
                    conn to result
                }
            }.awaitAll()

            val successCount = dispatchResults.count { it.second.isSuccess }
            val totalCount = dispatchResults.size

            if (successCount > 0) {
                val detailMessage = if (totalCount == 1) {
                    val single = dispatchResults.first()
                    "VitalStock (${single.first.name}): ${single.second.getOrDefault("OK")}"
                } else if (successCount == totalCount) {
                    "Enviado a ${successCount} PCs: " + dispatchResults.joinToString(", ") { "${it.first.name} (OK)" }
                } else {
                    "Enviado a ${successCount}/${totalCount} PCs • " + dispatchResults.joinToString("; ") { (conn, res) ->
                        if (res.isSuccess) "${conn.name}: OK" else "${conn.name}: Falló"
                    }
                }

                Log.i(TAG, "Pago despachado: $detailMessage")
                val successEntity = initialEntity.copy(
                    id = insertedId,
                    status = DispatchStatus.SUCCESS,
                    errorMessage = detailMessage
                )
                dao.updatePayment(successEntity)
                onComplete?.invoke(true)
            } else {
                val failureDetails = dispatchResults.joinToString("; ") { (conn, res) ->
                    val err = res.exceptionOrNull()?.localizedMessage ?: "Error de conexión"
                    "${conn.name}: $err"
                }
                Log.e(TAG, "Fallo al reenviar a todas las PCs: $failureDetails")
                val failedEntity = initialEntity.copy(
                    id = insertedId,
                    status = DispatchStatus.FAILED,
                    retryCount = 1,
                    errorMessage = failureDetails
                )
                dao.updatePayment(failedEntity)
                onComplete?.invoke(false)
            }
        }
    }

    /**
     * Reintenta el envío de un pago específico a todas las PCs activas.
     */
    suspend fun retryPayment(paymentId: Long): Boolean {
        val entity = dao.getPaymentById(paymentId) ?: return false
        val yapePayment = entity.toYapePayment()
        val activeConnections = preferences.getConnections().filter { it.isEnabled }

        if (activeConnections.isEmpty()) {
            dao.updatePayment(
                entity.copy(
                    status = DispatchStatus.FAILED,
                    errorMessage = "No hay PCs activas configuradas para reintentar"
                )
            )
            return false
        }

        val dispatchResults = activeConnections.map { conn ->
            scope.async {
                val result = client.sendPayment(conn.baseUrl, yapePayment)
                conn to result
            }
        }.awaitAll()

        val successCount = dispatchResults.count { it.second.isSuccess }
        val totalCount = dispatchResults.size

        return if (successCount > 0) {
            val detailMessage = if (totalCount == 1) {
                val single = dispatchResults.first()
                "VitalStock (${single.first.name}): ${single.second.getOrDefault("OK")}"
            } else if (successCount == totalCount) {
                "Enviado a ${successCount} PCs: " + dispatchResults.joinToString(", ") { "${it.first.name} (OK)" }
            } else {
                "Enviado a ${successCount}/${totalCount} PCs • " + dispatchResults.joinToString("; ") { (conn, res) ->
                    if (res.isSuccess) "${conn.name}: OK" else "${conn.name}: Falló"
                }
            }

            dao.updatePayment(
                entity.copy(
                    status = DispatchStatus.SUCCESS,
                    errorMessage = detailMessage
                )
            )
            true
        } else {
            val failureDetails = dispatchResults.joinToString("; ") { (conn, res) ->
                val err = res.exceptionOrNull()?.localizedMessage ?: "Error de conexión"
                "${conn.name}: $err"
            }

            dao.updatePayment(
                entity.copy(
                    status = DispatchStatus.FAILED,
                    retryCount = entity.retryCount + 1,
                    errorMessage = failureDetails
                )
            )
            false
        }
    }

    /**
     * Reintenta todos los pagos marcados como FAILED o PENDING.
     */
    suspend fun retryAllFailed(): Int {
        val failed = dao.getPaymentsByStatus(DispatchStatus.FAILED) +
                dao.getPaymentsByStatus(DispatchStatus.PENDING)
        var recovered = 0
        for (item in failed) {
            val ok = retryPayment(item.id)
            if (ok) recovered++
        }
        return recovered
    }

    suspend fun ping(baseUrl: String): Result<String> {
        return client.ping(baseUrl)
    }

    companion object {
        private const val TAG = "VitalNotifier-Dispatcher"
    }
}
