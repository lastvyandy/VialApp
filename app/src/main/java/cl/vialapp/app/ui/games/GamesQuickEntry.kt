package cl.vialapp.app.ui.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.R

/** Shared, theme-aware shortcut; keeps the original home feature card intact. */
@Composable
fun GamesQuickEntry(onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Card(
        onClick = onClick, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, c.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = c.surface)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(15.dp), color = c.primaryContainer) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_home_gamepad), contentDescription = null,
                        tint = c.onPrimaryContainer, modifier = Modifier.size(27.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Minijuegos", color = c.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Aprende jugando y supera tus récords.", color = c.onSurfaceVariant, fontSize = 13.sp)
            }
            Surface(shape = RoundedCornerShape(12.dp), color = c.primaryContainer) {
                Text("Jugar", Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = c.onPrimaryContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
