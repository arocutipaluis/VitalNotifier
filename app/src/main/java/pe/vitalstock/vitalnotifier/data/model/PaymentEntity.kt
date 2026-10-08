package pe.vitalstock.vitalnotifier.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DispatchStatus {
    PENDING,
    SUCCESS,
    FAILED
}

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remitente: String,
    val monto: Double,
    val fechaHora: String,
    val codigoSeg: String? = null,
    val mensajeRaw: String,
    val status: DispatchStatus = DispatchStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val errorMessage: String? = null
) {
    fun toYapePayment(): YapePayment = YapePayment(
        remitente = remitente,
        monto = monto,
        fechaHora = fechaHora,
        codigoSeg = codigoSeg,
        mensajeRaw = mensajeRaw
    )
}
