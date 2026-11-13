package cl.vialapp.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.R
import cl.vialapp.app.ui.mascot.MascotPortrait

/**
 * Saludo visual de la mascota elegida. Los identificadores del backend
 * siguen siendo carvier y mister_pelao (nombre visible: Don Pelao).
 */
@Composable
fun HomeMascotCard(
    mascotId: String,
    licenseClass: String?
) {
    val isDonPelao = mascotId == "mister_pelao"
    val characterName = if (isDonPelao) "Don Pelao" else "Carvier"
    val imageId = if (isDonPelao) {
        R.drawable.mascot_mister_pelao
    } else {
        R.drawable.mascot_carvier
    }
    val licenseName = licenseClass
        ?.trim()
        ?.removePrefix("Clase ")
        ?.uppercase()
        ?.takeIf { it == "B" || it == "C" }

    val phrase = if (isDonPelao) {
        if (licenseName != null) {
            "¡Vamos, máquina! Esa Clase $licenseName no se consigue sola."
        } else {
            "¡Vamos, máquina! Hoy toca demostrar lo que sabes."
        }
    } else {
        if (licenseName != null) {
            "¡Vamos por esa Clase $licenseName! Yo te acompaño."
        } else {
            "¡Un poquito de práctica hoy y avanzamos juntos!"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MascotPortrait(
                drawable = imageId,
                modifier = Modifier.size(width = 94.dp, height = 105.dp),
                cornerRadius = 20.dp,
                contentDescription = characterName
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TU COMPAÑERO DE RUTA",
                    color = Color(0xFF2563EB),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.size(5.dp))
                Text(
                    text = characterName,
                    color = Color(0xFF172033),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp
                )
                Spacer(modifier = Modifier.size(7.dp))
                Text(
                    text = phrase,
                    color = Color(0xFF64748B),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
