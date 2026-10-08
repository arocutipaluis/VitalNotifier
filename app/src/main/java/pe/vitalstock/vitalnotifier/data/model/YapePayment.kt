package pe.vitalstock.vitalnotifier.data.model

import com.google.gson.annotations.SerializedName

/**
 * Representa la estructura de datos JSON esperada por el software de escritorio VitalStock
 * en el endpoint POST http://<IP_DE_LA_PC>:5005/yape
 */
data class YapePayment(
    @SerializedName("remitente")
    val remitente: String,

    @SerializedName("monto")
    val monto: Double,

    @SerializedName("fecha_hora")
    val fechaHora: String,

    @SerializedName("codigo_seg")
    val codigoSeg: String? = null,

    @SerializedName("mensaje_raw")
    val mensajeRaw: String
)
