package pe.vitalstock.vitalnotifier.data.remote

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import pe.vitalstock.vitalnotifier.data.model.YapePayment
import java.io.IOException
import java.util.concurrent.TimeUnit

class VitalStockClient {

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Configuración de timeouts optimizada y tolerante para red Wi-Fi de la botica
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Envía la información del pago de Yape a VitalStock vía HTTP POST.
     * Endpoints soportados por VitalStock: /yape o /notificacion
     */
    suspend fun sendPayment(baseUrl: String, payment: YapePayment): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val cleanBase = baseUrl.trim().removeSuffix("/")
                val url = "$cleanBase/yape"
                val jsonPayload = gson.toJson(payment)
                val requestBody = jsonPayload.toRequestBody(jsonMediaType)

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", "VitalNotifier-Android/1.0")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string()?.trim().orEmpty()
                    if (response.isSuccessful) {
                        Result.success(if (body.isNotEmpty()) body else "Recibido por VitalStock")
                    } else {
                        Result.failure(
                            IOException("VitalStock respondió con error HTTP ${response.code}: $body")
                        )
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Realiza un test de conectividad tipo ping hacia la PC de VitalStock.
     * Endpoint: http://<IP>:5005/ping
     */
    suspend fun ping(baseUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanBase = baseUrl.trim().removeSuffix("/")
            val url = "$cleanBase/ping"
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("User-Agent", "VitalNotifier-Android/1.0")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string()?.trim().orEmpty()
                if (response.isSuccessful) {
                    Result.success(if (body.isNotEmpty()) body else "Conectado a VitalStock (HTTP 200)")
                } else {
                    Result.failure(
                        IOException("Respuesta no exitosa de VitalStock (HTTP ${response.code}): $body")
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
