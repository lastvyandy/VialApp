package cl.vialapp.app.ui.profile

// GUÍA DEL ARCHIVO:
// Elección de Clase B o C sin confundir ni borrar rutas de aprendizaje.

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.vialapp.app.ui.theme.vialColor

@Composable
fun LicenseClassPickerDialog(
    currentLicense: String?,
    isSaving: Boolean,
    error: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val current = currentLicense?.trim()?.uppercase()?.removePrefix("CLASE ")?.trim()
    var selected by remember(current) {
        mutableStateOf(current?.takeIf { it == "B" || it == "C" } ?: "B")
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Cambiar clase de licencia", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Elige qué licencia quieres preparar. Tus exámenes anteriores y tu XP se conservan."
                )
                Spacer(modifier = Modifier.height(6.dp))
                listOf(
                    Triple("B", "Clase B", "Automóviles y vehículos particulares"),
                    Triple("C", "Clase C", "Motocicletas y vehículos similares")
                ).forEach { (id, title, description) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isSaving) { selected = id }
                            .padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == id,
                            onClick = { selected = id },
                            enabled = !isSaving
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(title, fontWeight = FontWeight.SemiBold)
                            Text(description, color = vialColor(Color(0xFF64748B)))
                        }
                    }
                }
                if (!error.isNullOrBlank()) {
                    Text(error, color = vialColor(Color(0xFFB91C1C)))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving && selected != current,
                onClick = { onConfirm(selected) }
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text("Guardar")
                }
            }
        },
        dismissButton = {
            TextButton(enabled = !isSaving, onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(22.dp)
    )
}
