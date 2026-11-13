package cl.vialapp.app.ui.mascot

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.R

private val VialBlue = Color(0xFF2563EB)
private val VialDark = Color(0xFF172033)
private val VialGray = Color(0xFF64748B)

/**
 * Mascot IDs match the values that will be stored in the backend.
 * This composable does not store anything until onContinue is called.
 */
@Composable
fun MascotSelectionScreen(
    currentMascot: String? = null,
    isSaving: Boolean = false,
    error: String? = null,
    onContinue: (String) -> Unit
) {
    var selected by rememberSaveable(currentMascot) {
        mutableStateOf(currentMascot)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7FB))
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                text = "TU COMPAÑERO DE RUTA",
                color = VialBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "¿Quién te acompañará?",
                color = VialDark,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Elige tu mascota para estudiar, completar misiones y celebrar tus logros. Puedes cambiarla después.",
                color = VialGray,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(24.dp))

            MascotOption(
                name = "Carvier",
                description = "Amable, alegre y siempre listo para ayudarte.",
                drawable = R.drawable.mascot_carvier,
                selected = selected == "carvier",
                onSelect = { selected = "carvier" }
            )
            Spacer(Modifier.height(14.dp))
            MascotOption(
                name = "Don Pelao",
                description = "Musculoso, exigente y fanático de los desafíos.",
                drawable = R.drawable.mascot_mister_pelao,
                selected = selected == "mister_pelao",
                onSelect = { selected = "mister_pelao" }
            )

            if (!error.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = error,
                    color = Color(0xFFDC2626),
                    fontSize = 13.sp
                )
            }
        }

        Surface(
            color = Color.White,
            shadowElevation = 6.dp
        ) {
            Button(
                onClick = { selected?.let(onContinue) },
                enabled = selected != null && !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VialBlue)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text("Continuar con mi mascota", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MascotOption(
    name: String,
    description: String,
    drawable: Int,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) VialBlue else Color(0xFFE2E8F0)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Image(
                painter = painterResource(drawable),
                contentDescription = "Mascota $name",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) VialBlue else Color(0xFFE2E8F0),
                        shape = RoundedCornerShape(28.dp)
                    )
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = VialDark,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = description,
                    color = VialGray,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
            RadioButton(
                selected = selected,
                onClick = onSelect,
                modifier = Modifier.size(30.dp),
                colors = RadioButtonDefaults.colors(selectedColor = VialBlue)
            )
        }
    }
}
