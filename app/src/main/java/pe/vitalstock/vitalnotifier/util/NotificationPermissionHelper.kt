package pe.vitalstock.vitalnotifier.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import pe.vitalstock.vitalnotifier.service.YapeNotificationListenerService

object NotificationPermissionHelper {

    /**
     * Verifica si el usuario ya otorgó el permiso especial de Acceso a Notificaciones
     * a nuestra aplicación.
     */
    fun isNotificationAccessGranted(context: Context): Boolean {
        val packageName = context.packageName
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        if (!flat.isNullOrEmpty()) {
            val names = flat.split(":")
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && cn.packageName == packageName) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Retorna el Intent adecuado para abrir la pantalla del sistema de Android donde el usuario
     * activa el permiso de escucha de notificaciones para VitalNotifier.
     */
    fun createNotificationAccessSettingsIntent(context: Context): Intent {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val componentName = ComponentName(context, YapeNotificationListenerService::class.java)
            intent.putExtra(
                ":settings:fragment_args_key",
                componentName.flattenToString()
            )
        }
        return intent
    }

    /**
     * Verifica si la app está exenta del ahorro agresivo de batería de Android.
     */
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            return pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        }
        return true
    }

    /**
     * Retorna el intent para pedir al usuario que excluya a VitalNotifier del ahorro de batería.
     */
    fun createIgnoreBatteryOptimizationIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
    }
}
