package pe.vitalstock.vitalnotifier.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import pe.vitalstock.vitalnotifier.R
import pe.vitalstock.vitalnotifier.data.model.YapePayment
import java.util.Locale

object SoundAlertHelper {

    private const val CHANNEL_ID = "vitalnotifier_payments_channel"
    private const val CHANNEL_NAME = "Alertas de Pago Yape"

    /**
     * Emite un sonido de alerta claro, fuerte y profesional tipo "caja registradora / confirmación",
     * acompañado de una vibración distintiva y una notificación flotante (heads-up) en pantalla.
     */
    fun playPaymentReceivedAlert(context: Context, payment: YapePayment) {
        try {
            // 1. Sonido de confirmación nítido (Beep doble de alta fidelidad)
            playToneBeep()

            // 2. Timbre de notificación del sistema
            playSystemNotificationSound(context)

            // 3. Patrón de vibración táctil para el mostrador (Vibrar - pausa - vibrar)
            vibrateDevice(context)

            // 4. Notificación visual flotante (Heads-Up) en la barra de Android
            showHeadsUpNotification(context, payment)
        } catch (e: Exception) {
            Log.e("SoundAlertHelper", "Error al emitir alerta de sonido: ${e.message}")
        }
    }

    private fun playToneBeep() {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) // Volumen máximo
            // TONE_PROP_BEEP2 o TONE_CDMA_CONFIRM: tono doble claro y nítido
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
        } catch (e: Exception) {
            Log.w("SoundAlertHelper", "No se pudo reproducir ToneGenerator: ${e.message}")
        }
    }

    private fun playSystemNotificationSound(context: Context) {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, notificationUri)
            ringtone?.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            ringtone?.play()
        } catch (e: Exception) {
            Log.w("SoundAlertHelper", "No se pudo reproducir Ringtone: ${e.message}")
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrateDevice(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrationEffect = VibrationEffect.createWaveform(longArrayOf(0, 180, 80, 220), -1)
                vibratorManager?.vibrate(CombinedVibration.createParallel(vibrationEffect))
            } else {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 180, 80, 220), -1))
                } else {
                    vibrator?.vibrate(longArrayOf(0, 180, 80, 220), -1)
                }
            }
        } catch (e: Exception) {
            Log.w("SoundAlertHelper", "Vibración no disponible: ${e.message}")
        }
    }

    private fun showHeadsUpNotification(context: Context, payment: YapePayment) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal de notificación de alta importancia en Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones emergentes con sonido de pagos capturados de Yape"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val codeInfo = if (!payment.codigoSeg.isNullOrBlank()) " • Cód: ${payment.codigoSeg}" else ""
        val contentText = "${payment.remitente} • ${payment.fechaHora}$codeInfo"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("¡Pago Yape S/ ${String.format(Locale.US, "%.2f", payment.monto)} recibido!")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Monto: S/ ${String.format(Locale.US, "%.2f", payment.monto)}\nCliente: ${payment.remitente}\nReenviado a VitalStock Desktop"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }
}
