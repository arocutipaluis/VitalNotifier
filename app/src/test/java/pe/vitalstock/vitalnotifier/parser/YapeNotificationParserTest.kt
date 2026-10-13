package pe.vitalstock.vitalnotifier.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YapeNotificationParserTest {

    @Test
    fun testParseClassicYapeFormat() {
        val message = "¡Te transfirieron S/ 15.50 de Juan Perez!"
        val payment = YapeNotificationParser.parse(title = "Yape", text = message)

        assertNotNull(payment)
        assertEquals(15.50, payment!!.monto, 0.001)
        assertEquals("Juan Perez", payment.remitente)
        assertEquals(message, payment.mensajeRaw.trim().replace("Yape ", ""))
    }

    @Test
    fun testParseSenderBeforeAction() {
        val title = "Yape"
        val text = "Juan Perez te transfirió S/ 15.50"
        val payment = YapeNotificationParser.parse(title = title, text = text)

        assertNotNull(payment)
        assertEquals(15.50, payment!!.monto, 0.001)
        assertEquals("Juan Perez", payment.remitente)
    }

    @Test
    fun testParseWithSecurityCode() {
        val title = "Confirmación de pago"
        val text = "Juan Carlos Perez te envió S/ 25.00 con código de seguridad 849201"
        val payment = YapeNotificationParser.parse(title = title, text = text)

        assertNotNull(payment)
        assertEquals(25.00, payment!!.monto, 0.001)
        assertEquals("Juan Carlos Perez", payment.remitente)
        assertEquals("849201", payment.codigoSeg)
    }

    @Test
    fun testParseWithShortCod() {
        val title = "Yape"
        val text = "Has recibido S/ 35.00 de Pedro Quispe. Cód: 981240"
        val payment = YapeNotificationParser.parse(title = title, text = text)

        assertNotNull(payment)
        assertEquals(35.00, payment!!.monto, 0.001)
        assertEquals("Pedro Quispe", payment.remitente)
        assertEquals("981240", payment.codigoSeg)
    }

    @Test
    fun testParseTitleAsSender() {
        val title = "María Lopez"
        val text = "Te envió S/ 50.00"
        val payment = YapeNotificationParser.parse(title = title, text = text)

        assertNotNull(payment)
        assertEquals(50.00, payment!!.monto, 0.001)
        assertEquals("María Lopez", payment.remitente)
    }

    @Test
    fun testIsYapeNotificationDetection() {
        assertTrue(
            YapeNotificationParser.isYapeNotification(
                packageName = "com.bcp.innovacxion.yapeapp",
                title = "Alerta",
                text = "Recibiste dinero"
            )
        )

        assertTrue(
            YapeNotificationParser.isYapeNotification(
                packageName = "com.other.app",
                title = "Notificación",
                text = "¡Te transfirieron S/ 10.00 de Carlos!"
            )
        )

        // No debe detectar notificaciones genéricas no financieras
        val isOther = YapeNotificationParser.isYapeNotification(
            packageName = "com.whatsapp",
            title = "Juan",
            text = "Hola, te paso a ver a las 5"
        )
        org.junit.Assert.assertFalse(isOther)
    }

    @Test
    fun testParseInvalidNotificationReturnsNull() {
        val payment = YapeNotificationParser.parse(
            title = "Mensaje",
            text = "Tu código de verificación es 4567"
        )
        assertNull(payment)
    }
}
