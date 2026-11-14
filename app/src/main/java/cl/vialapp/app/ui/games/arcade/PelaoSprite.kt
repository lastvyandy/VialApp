package cl.vialapp.app.ui.games.arcade

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import cl.vialapp.app.R

/**
 * Optional transparent arena sprites. Android resolves drawable names without the extension,
 * so both WebP and PNG work. Missing files never stop the build.
 */
@SuppressLint("DiscouragedApi")
@Composable
internal fun PelaoSprite(pose: PelaoPose, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val resources = context.resources
    // Resolve once, not every frame. No image generation or decoding in the simulation engine.
    val ids = remember(resources, context.packageName) {
        fun lookup(name: String) =
            resources.getIdentifier(name, "drawable", context.packageName).takeIf { it != 0 }
        val idle = lookup(PelaoPose.IDLE.resourceName) ?: R.drawable.mascot_mister_pelao
        val firstAttack = lookup(PelaoPose.ATTACK_ONE.resourceName)
            ?: lookup("don_pelao_attack")
            ?: lookup(PelaoPose.ATTACK_TWO.resourceName)
            ?: lookup("don_pelao_happy")
            ?: idle
        mapOf(
            PelaoPose.IDLE to idle,
            PelaoPose.ATTACK_ONE to firstAttack,
            PelaoPose.ATTACK_TWO to (lookup(PelaoPose.ATTACK_TWO.resourceName) ?: firstAttack),
            PelaoPose.HIT to (lookup(PelaoPose.HIT.resourceName) ?: lookup("don_pelao_disappointed") ?: idle),
            PelaoPose.BLOCK to (lookup(PelaoPose.BLOCK.resourceName) ?: lookup("don_pelao_serious") ?: idle),
            PelaoPose.VICTORY to (lookup(PelaoPose.VICTORY.resourceName) ?: lookup("don_pelao_celebrating") ?: idle),
            PelaoPose.DEFEAT to (lookup(PelaoPose.DEFEAT.resourceName) ?: lookup("don_pelao_disappointed") ?: idle)
        )
    }
    Image(
        painter = painterResource(ids.getValue(pose)),
        contentDescription = when (pose) {
            PelaoPose.IDLE -> "Don Pelao en guardia"
            PelaoPose.ATTACK_ONE, PelaoPose.ATTACK_TWO -> "Don Pelao golpeando"
            PelaoPose.HIT -> "Don Pelao recibiendo daño"
            PelaoPose.BLOCK -> "Don Pelao bloqueando"
            PelaoPose.VICTORY -> "Don Pelao celebra su victoria"
            PelaoPose.DEFEAT -> "Don Pelao derrotado"
        },
        modifier = modifier,
        contentScale = ContentScale.Fit,
        alignment = Alignment.BottomCenter
    )
}
