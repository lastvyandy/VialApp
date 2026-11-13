package cl.vialapp.app.ui.mascot

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cl.vialapp.app.R

/**
 * Retrato compartido por el selector y Perfil.
 *
 * Don Pelao está dibujado en formato vertical, con la cabeza arriba.
 * Amplificamos su parte superior sin recortar la calva ni centrar la cintura.
 * No modificamos los archivos originales ni los IDs del backend.
 */
@Composable
fun MascotPortrait(
    drawable: Int,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    cornerRadius: Dp = 24.dp,
    contentDescription: String? = null
) {
    val shape = RoundedCornerShape(cornerRadius)
    val isDonPelao = drawable == R.drawable.mascot_mister_pelao

    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFFF1F5F9))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color(0xFF2563EB) else Color(0xFFE2E8F0),
                shape = shape
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Image(
            painter = painterResource(drawable),
            contentDescription = contentDescription,
            contentScale = if (isDonPelao) ContentScale.Crop else ContentScale.Fit,
            alignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (isDonPelao) {
                        // El zoom se ancla arriba para destacar la cabeza y el rostro.
                        scaleX = 1.30f
                        scaleY = 1.30f
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    }
                }
        )
    }
}
