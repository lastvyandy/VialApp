package cl.vialapp.app.ui.mascot

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.R
import cl.vialapp.app.ui.theme.vialColor

@Composable
@SuppressLint("DiscouragedApi") // Recursos opcionales: se incorporarán sin romper builds anteriores.
fun MascotReactionCard(
    mascotId: String,
    reaction: MascotReaction,
    levelReached: Int? = null,
    modifier: Modifier = Modifier,
    embedded: Boolean = false
) {
    if (!MascotReactions.isSupported(mascotId)) return

    val context = LocalContext.current
    val resourceName = MascotReactions.resourceName(mascotId, reaction)
    val reactionDrawable = remember(context, resourceName) {
        context.resources.getIdentifier(resourceName, "drawable", context.packageName)
    }
    val name = MascotReactions.name(mascotId)
    val fallback = if (mascotId == "mister_pelao") R.drawable.mascot_mister_pelao
        else R.drawable.mascot_carvier
    val portraitModifier = Modifier.size(width = 94.dp, height = 105.dp)
    val shape = RoundedCornerShape(18.dp)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (embedded) Color.Transparent else vialColor(Color.White))
    ) {
        Row(
            modifier = Modifier.padding(if (embedded) 0.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (reactionDrawable != 0) {
                Image(
                    painter = painterResource(reactionDrawable),
                    contentDescription = name,
                    modifier = portraitModifier
                        .clip(shape)
                        .background(vialColor(Color(0xFFF1F5F9))),
                    contentScale = ContentScale.Fit
                )
            } else {
                MascotPortrait(
                    drawable = fallback,
                    modifier = portraitModifier,
                    cornerRadius = 18.dp,
                    contentDescription = name
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = name,
                    color = vialColor(Color(0xFF172033)),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = MascotReactions.message(mascotId, reaction),
                    color = vialColor(Color(0xFF475569)),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                if (levelReached != null) {
                    Text(
                        text = "¡Nivel $levelReached alcanzado!",
                        color = vialColor(Color(0xFF15803D)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
