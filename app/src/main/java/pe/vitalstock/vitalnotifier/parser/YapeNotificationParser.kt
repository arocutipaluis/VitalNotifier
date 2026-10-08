package pe.vitalstock.vitalnotifier.parser

import pe.vitalstock.vitalnotifier.data.model.YapePayment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

object YapeNotificationParser {

    const val YAPE_PACKAGE_NAME = "com.bcp.innovacxion.yapeapp"

    // Palabras clave que identifican inequívocamente una transacción de Yape
    private val YAPE_KEYWORDS = listOf(
        "te transfirió",
        "te transfirieron",
        "te envió",
        "te yapeó",
        "te yapearon",
        "recibiste s/",
        "has recibido s/",
        "confirmación de pago",
        "pago recibido"
    )

    // Regex para capturar montos en soles peruanos: S/ 15.50, S/15.50, S/. 15.00, S/ 20, etc.
    private val AMOUNT_PATTERN = Pattern.compile(
        """(?:S\/?\.?|Soles)\s*([0-9]+(?:[.,][0-9]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    // Regex para capturar códigos de validación / seguridad si están presentes
    private val SECURITY_CODE_PATTERN = Pattern.compile(
        """(?:código(?: de seguridad| de validación)?|cód\.?\s*seg\.?|cód\.?|cod\.?)\s*[:#]?\s*([0-9a-zA-Z]{4,10})""",
        Pattern.CASE_INSENSITIVE
    )

    // Regex para capturar remitente precedido por "de" o "por": "... S/ 15.50 de Juan Perez!"
    private val SENDER_AFTER_DE_PATTERN = Pattern.compile(
        """\b(?:de|por)\s+([A-Za-zÁÉÍÓÚáéíóúÑñ\s\.\'\-]{3,45}?)(?:\s*(?:con\s+código|cód|\.|\!|\?|$))""",
        Pattern.CASE_INSENSITIVE
    )

    // Regex para capturar remitente antes de la acción: "Juan Perez te transfirió S/ 15.50"
    private val SENDER_BEFORE_ACTION_PATTERN = Pattern.compile(
        """(?:^|[!¡\n])\s*([A-Za-zÁÉÍÓÚáéíóúÑñ\s\.\'\-]{3,40}?)\s+(?:te transfirió|te transfirieron|te envió|te yapeó|te yapearon)""",
        Pattern.CASE_INSENSITIVE
    )

    private val GENERIC_TITLES = setOf(
        "yape",
        "bcp",
        "confirmación de pago",
        "pago recibido",
        "notificación",
        "alerta"
    )

    /**
     * Determina si la notificación recibida pertenece a Yape o contiene texto de pago financiero.
     */
    fun isYapeNotification(packageName: String?, title: String?, text: String?): Boolean {
        val fullContent = "${title.orEmpty()} ${text.orEmpty()}".lowercase(Locale.ROOT)

        val isFromYapePackage = packageName == YAPE_PACKAGE_NAME
        val hasYapeKeywords = YAPE_KEYWORDS.any { fullContent.contains(it) }
        val hasCurrencySignal = fullContent.contains("s/") || fullContent.contains("s/.") || fullContent.contains("soles")

        return isFromYapePackage || (hasYapeKeywords && hasCurrencySignal)
    }

    /**
     * Parsea el título y texto de la notificación para extraer los campos requeridos por VitalStock.
     * Retorna null si no se pudo identificar como un pago válido con monto.
     */
    fun parse(
        title: String?,
        text: String?,
        postTimeMillis: Long = System.currentTimeMillis()
    ): YapePayment? {
        val cleanTitle = title?.trim().orEmpty()
        val cleanText = text?.trim().orEmpty()

        if (cleanTitle.isEmpty() && cleanText.isEmpty()) {
            return null
        }

        val fullMessage = when {
            cleanTitle.isNotEmpty() && cleanText.isNotEmpty() -> "$cleanTitle $cleanText"
            cleanTitle.isNotEmpty() -> cleanTitle
            else -> cleanText
        }

        // 1. Extraer Monto
        val amount = extractAmount(fullMessage) ?: return null

        // 2. Extraer Remitente
        val sender = extractSender(cleanTitle, cleanText, fullMessage)

        // 3. Extraer Código de Seguridad / Validación
        val securityCode = extractSecurityCode(fullMessage)

        // 4. Formatear Fecha y Hora
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(postTimeMillis))

        return YapePayment(
            remitente = sender,
            monto = amount,
            fechaHora = formattedDate,
            codigoSeg = securityCode,
            mensajeRaw = fullMessage
        )
    }

    private fun extractAmount(text: String): Double? {
        val matcher = AMOUNT_PATTERN.matcher(text)
        if (matcher.find()) {
            val rawNumber = matcher.group(1)?.replace(",", ".") ?: return null
            return rawNumber.toDoubleOrNull()
        }
        return null
    }

    private fun extractSecurityCode(text: String): String? {
        val matcher = SECURITY_CODE_PATTERN.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.trim()
        }
        return null
    }

    private fun extractSender(title: String, text: String, fullMessage: String): String {
        // Paso 1: Buscar remitente dentro de 'text' (donde comúnmente está la frase de la transacción)
        val matcherDeText = SENDER_AFTER_DE_PATTERN.matcher(text)
        if (matcherDeText.find()) {
            val candidate = cleanSender(matcherDeText.group(1))
            if (isValidSender(candidate)) return candidate
        }

        val matcherBeforeActionText = SENDER_BEFORE_ACTION_PATTERN.matcher(text)
        if (matcherBeforeActionText.find()) {
            val candidate = cleanSender(matcherBeforeActionText.group(1))
            if (isValidSender(candidate)) return candidate
        }

        // Paso 2: Si el título no es genérico, el título mismo es el nombre del remitente (ej: "María Lopez")
        if (title.isNotEmpty() && !isGenericTitle(title)) {
            val candidate = cleanSender(title)
            if (isValidSender(candidate)) return candidate
        }

        // Paso 3: Fallback en mensaje completo
        val matcherDeFull = SENDER_AFTER_DE_PATTERN.matcher(fullMessage)
        if (matcherDeFull.find()) {
            val candidate = cleanSender(matcherDeFull.group(1))
            if (isValidSender(candidate)) return candidate
        }

        val matcherBeforeActionFull = SENDER_BEFORE_ACTION_PATTERN.matcher(fullMessage)
        if (matcherBeforeActionFull.find()) {
            val candidate = cleanSender(matcherBeforeActionFull.group(1))
            if (isValidSender(candidate)) return candidate
        }

        return "Cliente Yape"
    }

    private fun cleanSender(raw: String?): String {
        if (raw == null) return ""
        return raw
            .replace(Regex("""^(?:yape|bcp)\s+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^[\s!¡\-:;,\.]+"""), "")
            .replace(Regex("""[\s!¡\-:;,\.]+$"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun isGenericTitle(title: String): Boolean {
        val lower = title.lowercase(Locale.ROOT)
        return GENERIC_TITLES.any { lower.contains(it) }
    }

    private fun isValidSender(candidate: String): Boolean {
        if (candidate.length < 2) return false
        val lower = candidate.lowercase(Locale.ROOT)
        val blacklistedWords = listOf("yape", "soles", "seguridad", "código", "bcp", "transferencia")
        return !blacklistedWords.any { lower == it }
    }
}
