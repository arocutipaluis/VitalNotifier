package pe.vitalstock.vitalnotifier.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.vitalstock.vitalnotifier.VitalNotifierApp
import pe.vitalstock.vitalnotifier.parser.YapeNotificationParser

class YapeNotificationListenerService : NotificationListenerService() {

    // Cache para evitar procesar la misma notificación duplicada si el sistema la refresca
    private var lastProcessedSignature: String? = null
    private var lastProcessedTimestamp: Long = 0L

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isServiceConnected.value = true
        Log.i(TAG, "Servicio NotificationListenerService conectado y activo.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isServiceConnected.value = false
        Log.w(TAG, "Servicio NotificationListenerService desconectado.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Extraer todos los campos de texto posibles de la notificación de Android
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
        val tickerText = notification.tickerText?.toString()

        // 1. Manejo crucial de NOTIFICACIONES AGRUPADAS / APILADAS (InboxStyle / Stacked):
        // Cuando llega un segundo o tercer Yape consecutivo, Android agrupa las notificaciones
        // y coloca las líneas individuales en EXTRA_TEXT_LINES.
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        val latestLineInStack = textLines?.lastOrNull()?.toString()

        // Recopilar candidatos de texto para evaluar
        val candidateTexts = mutableListOf<String>()

        // Si hay líneas apiladas, la más reciente tiene prioridad
        if (!latestLineInStack.isNullOrBlank()) {
            candidateTexts.add(latestLineInStack)
        }
        if (!bigText.isNullOrBlank()) {
            candidateTexts.add(bigText)
        }
        if (!text.isNullOrBlank()) {
            candidateTexts.add(text)
        }
        if (!tickerText.isNullOrBlank()) {
            candidateTexts.add(tickerText)
        }
        if (!summaryText.isNullOrBlank()) {
            candidateTexts.add(summaryText)
        }

        // Si hay más líneas en la pila, agregarlas también como respaldo
        if (textLines != null && textLines.size > 1) {
            for (i in textLines.indices.reversed()) {
                val l = textLines[i]?.toString()
                if (!l.isNullOrBlank() && !candidateTexts.contains(l)) {
                    candidateTexts.add(l)
                }
            }
        }

        // Buscar el primer texto que sea un pago Yape válido
        var foundPayment: pe.vitalstock.vitalnotifier.data.model.YapePayment? = null
        var matchedText = ""

        for (candidate in candidateTexts) {
            val isYape = YapeNotificationParser.isYapeNotification(
                packageName = packageName,
                title = title,
                text = candidate
            )

            if (isYape) {
                val parsed = YapeNotificationParser.parse(
                    title = title,
                    text = candidate,
                    postTimeMillis = sbn.postTime
                )
                if (parsed != null) {
                    foundPayment = parsed
                    matchedText = candidate
                    break
                }
            }
        }

        if (foundPayment == null) {
            // No se detectó un pago en ningún candidato
            return
        }

        // Deduplicación inteligente: evitar reenviar el mismo pago exacto si Android
        // emite un evento de actualización del grupo en menos de 4 segundos
        val signature = "${foundPayment.remitente}_${foundPayment.monto}_${foundPayment.codigoSeg}"
        val now = System.currentTimeMillis()
        if (signature == lastProcessedSignature && (now - lastProcessedTimestamp) < 4000L) {
            Log.d(TAG, "Notificación duplicada ignorada (refresco de grupo en Android): $signature")
            return
        }

        lastProcessedSignature = signature
        lastProcessedTimestamp = now

        Log.i(TAG, "¡Nuevo pago capturado (posible grupo/pila)! Monto: S/ ${foundPayment.monto}, Remitente: ${foundPayment.remitente}, Texto: '$matchedText'")

        // Despachar inmediatamente a VitalStock
        VitalNotifierApp.instance.paymentDispatcher.processAndDispatch(foundPayment)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    companion object {
        private const val TAG = "VitalNotifier-Listener"

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected = _isServiceConnected.asStateFlow()
    }
}
