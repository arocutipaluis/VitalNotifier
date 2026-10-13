package pe.vitalstock.vitalnotifier.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.vitalstock.vitalnotifier.data.model.ServerConnection
import pe.vitalstock.vitalnotifier.ui.theme.TealPrimary

@Composable
fun EditConnectionDialog(
    initialConnection: ServerConnection? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, ip: String, port: Int) -> Unit
) {
    var name by remember { mutableStateOf(initialConnection?.name ?: "") }
    var ip by remember { mutableStateOf(initialConnection?.ip ?: "") }
    var portText by remember { mutableStateOf(initialConnection?.port?.toString() ?: "5005") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val isEditing = initialConnection != null

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF0F172A),
        unfocusedTextColor = Color(0xFF0F172A),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedLabelColor = TealPrimary,
        unfocusedLabelColor = Color(0xFF64748B),
        cursorColor = TealPrimary,
        focusedBorderColor = TealPrimary,
        unfocusedBorderColor = Color(0xFFCBD5E1)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Editar Conexión de PC" else "Añadir Nueva PC de VitalStock",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF0F172A)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Configura la dirección de la computadora que tiene VitalStock abierto.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre o Ubicación") },
                    placeholder = { Text("Ej: Caja 1, Caja 2, Mostrador", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = ip,
                    onValueChange = {
                        ip = it
                        errorText = null
                    },
                    label = { Text("IP de la PC") },
                    placeholder = { Text("Ej: 192.168.100.36", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    colors = textFieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = errorText != null,
                    supportingText = errorText?.let { { Text(it, color = Color.Red) } },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it },
                    label = { Text("Puerto") },
                    placeholder = { Text("5005", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    colors = textFieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanIp = ip.trim().removeSuffix("/")
                    if (cleanIp.isBlank()) {
                        errorText = "Ingresa la dirección IP de la PC"
                        return@Button
                    }
                    val parsedPort = portText.trim().toIntOrNull() ?: 5005
                    val cleanName = name.trim().ifBlank { "PC VitalStock" }
                    onSave(cleanName, cleanIp, parsedPort)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text(if (isEditing) "Actualizar" else "Guardar PC")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFF64748B))
            }
        }
    )
}
