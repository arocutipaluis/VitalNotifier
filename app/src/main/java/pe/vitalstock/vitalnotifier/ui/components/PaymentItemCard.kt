package pe.vitalstock.vitalnotifier.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.vitalstock.vitalnotifier.data.model.DispatchStatus
import pe.vitalstock.vitalnotifier.data.model.PaymentEntity
import pe.vitalstock.vitalnotifier.ui.theme.ErrorRed
import pe.vitalstock.vitalnotifier.ui.theme.ErrorRedBg
import pe.vitalstock.vitalnotifier.ui.theme.SuccessGreen
import pe.vitalstock.vitalnotifier.ui.theme.SuccessGreenBg
import pe.vitalstock.vitalnotifier.ui.theme.WarningOrange
import pe.vitalstock.vitalnotifier.ui.theme.WarningOrangeBg
import java.util.Locale

@Composable
fun PaymentItemCard(
    payment: PaymentEntity,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val (statusColor, statusBg, statusText, statusIcon) = when (payment.status) {
        DispatchStatus.SUCCESS -> Quadruple(
            SuccessGreen,
            SuccessGreenBg,
            "ENVIADO A VITALSTOCK",
            Icons.Default.CheckCircle
        )
        DispatchStatus.FAILED -> Quadruple(
            ErrorRed,
            ErrorRedBg,
            "FALLO DE ENVÍO",
            Icons.Default.Error
        )
        DispatchStatus.PENDING -> Quadruple(
            WarningOrange,
            WarningOrangeBg,
            "EN COLA",
            Icons.Default.HourglassTop
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Fila superior: Remitente y Monto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = payment.remitente,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = payment.fechaHora,
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Text(
                    text = String.format(Locale.US, "S/ %.2f", payment.monto),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0D9488)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Fila intermedia: Badge de estado y código de seguridad
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge de estado
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }

                // Código de validación si existe
                if (!payment.codigoSeg.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Cód: ${payment.codigoSeg}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }

            // Si falló, mostrar botón de reintentar
            if (payment.status == DispatchStatus.FAILED) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = payment.errorMessage ?: "Fallo de conexión",
                        fontSize = 11.sp,
                        color = ErrorRed,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = onRetry,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reintentar",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Reintentar", fontSize = 11.sp)
                    }
                }
            }

            // Mensaje RAW desplegable al hacer clic
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Texto original de notificación (RAW):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = payment.mensajeRaw,
                        fontSize = 11.sp,
                        color = Color(0xFF334155)
                    )
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
