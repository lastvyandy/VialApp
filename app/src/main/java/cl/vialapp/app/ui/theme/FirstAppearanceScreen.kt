package cl.vialapp.app.ui.theme

// GUÍA DEL ARCHIVO:
// Primera elección de apariencia antes de entrar a estudiar.

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun FirstAppearanceScreen(initialDark: Boolean, onContinue: (Boolean) -> Unit) {
    var selectedDark by rememberSaveable { mutableStateOf(initialDark) }
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(20.dp))
            Text("VialApp", color = scheme.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Elige tu apariencia", fontSize = 29.sp, fontWeight = FontWeight.Bold, color = scheme.onBackground)
            Text(
                "Personaliza cómo quieres ver la aplicación. Puedes cambiarlo más adelante desde Perfil.",
                fontSize = 15.sp, color = scheme.onSurfaceVariant
            )
            AppearanceOption(false, !selectedDark) { selectedDark = false }
            AppearanceOption(true, selectedDark) { selectedDark = true }
            Spacer(Modifier.height(12.dp))
        }
        Button(
            onClick = { onContinue(selectedDark) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VialActionBlue, contentColor = Color.White)
        ) {
            Text("Continuar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun AppearanceOption(dark: Boolean, selected: Boolean, onSelect: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val previewBackground = if (dark) Color(0xFF101827) else Color(0xFFF4F7FB)
    val previewCard = if (dark) Color(0xFF293E60) else Color.White
    val previewText = if (dark) Color(0xFFF1F5F9) else Color(0xFF172033)
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = Modifier.fillMaxWidth().clip(shape).selectable(
            selected = selected, onClick = onSelect, role = Role.RadioButton
        ),
        shape = shape,
        color = scheme.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else scheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier.width(92.dp).clip(RoundedCornerShape(12.dp))
                    .background(previewBackground).padding(9.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Inicio", color = previewText, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Box(Modifier.fillMaxWidth().height(23.dp).clip(RoundedCornerShape(6.dp)).background(VialActionBlue))
                repeat(2) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(21.dp)
                            .clip(RoundedCornerShape(6.dp)).background(previewCard).padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF60A5FA)))
                        Spacer(Modifier.width(5.dp))
                        Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(previewText.copy(alpha = .65f)))
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text(if (dark) "🌙 Oscuro" else "☀️ Claro", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(if (dark) "Colores nocturnos y menor brillo" else "Fondos claros y colores luminosos", fontSize = 13.sp, color = scheme.onSurfaceVariant)
            }
            RadioButton(selected = selected, onClick = null)
        }
    }
}
