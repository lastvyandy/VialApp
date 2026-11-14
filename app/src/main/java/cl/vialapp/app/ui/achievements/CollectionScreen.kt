package cl.vialapp.app.ui.achievements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.R
import cl.vialapp.app.data.api.AchievementItem
import cl.vialapp.app.data.api.ApiClient
import cl.vialapp.app.data.session.SessionManager
import kotlinx.coroutines.CancellationException

@Composable
fun CollectionEntryCard(onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, c.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = c.surface)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = Color(0xFF233B68), shape = RoundedCornerShape(16.dp)) {
                Icon(painterResource(R.drawable.ic_collection_medal), contentDescription = null,
                    modifier = Modifier.padding(12.dp).size(30.dp), tint = Color(0xFFFFD778))
            }
            Column(Modifier.weight(1f)) {
                Text("Mi colección", color = c.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Medallas y cartas desbloqueables", color = c.onSurfaceVariant, fontSize = 13.sp)
            }
            Text("Ver ›", color = c.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
fun CollectionScreen(userKey: String, onBack: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current
    val session = remember(context) { SessionManager(context) }
    var data by remember(userKey) { mutableStateOf<List<AchievementItem>>(emptyList()) }
    var newly by remember(userKey) { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember(userKey) { mutableStateOf(true) }
    var error by remember(userKey) { mutableStateOf("") }
    var refresh by remember(userKey) { mutableIntStateOf(0) }
    var kind by rememberSaveable(userKey) { mutableStateOf("medal") }
    var selected by remember(userKey) { mutableStateOf<AchievementItem?>(null) }
    LaunchedEffect(userKey, refresh) {
        loading = true
        error = ""
        try {
            val token = session.getAccessToken()
            if (token.isNullOrBlank()) {
                error = "Inicia sesión para consultar tu colección."
            } else {
                val response = ApiClient.api.syncAchievements("Bearer $token")
                val collection = response.body()
                if (response.isSuccessful && collection != null) {
                    data = collection.items
                    newly = collection.newlyUnlocked
                } else {
                    error = "No se pudo sincronizar la colección. Inténtalo de nuevo."
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = "No se pudo conectar con VialApp."
        } finally {
            loading = false
        }
    }
    val unlocked = data.count { it.unlocked }
    val medals = data.filter { it.kind == "medal" }
    val cards = data.filter { it.kind == "collectible" }
    val shown = if (kind == "medal") medals else cards

    LazyColumn(Modifier.fillMaxSize().background(c.background),
        contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) { Text("‹ Volver") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { refresh++ }, enabled = !loading) { Text("Actualizar") }
            }
            Text("Mi colección", fontSize = 29.sp, fontWeight = FontWeight.ExtraBold, color = c.onSurface)
            Text("Cada desafío deja una huella.", fontSize = 14.sp, color = c.onSurfaceVariant)
        }
        if (loading && data.isEmpty()) item {
            Box(Modifier.fillMaxWidth().height(210.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        if (error.isNotBlank()) item {
            Surface(color = c.errorContainer, shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(error, color = c.onErrorContainer)
                    TextButton(onClick = { refresh++ }) { Text("Reintentar") }
                }
            }
        }
        if (data.isNotEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF183B60), Color(0xFF304F86))), RoundedCornerShape(24.dp)).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("TU VITRINA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBFDBFE))
                            Text("$unlocked / ${data.size}", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text("Piezas desbloqueadas", color = Color(0xFFDCE7F6), fontSize = 13.sp)
                        }
                        Icon(painterResource(R.drawable.ic_collection_medal), null, tint = Color(0xFFFFD778), modifier = Modifier.size(54.dp))
                    }
                    LinearProgressIndicator(progress = { unlocked.toFloat() / data.size.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth().height(8.dp), color = Color(0xFFFFD778), trackColor = Color(0xFF577194))
                }
            }
            if (newly.isNotEmpty()) item {
                Surface(shape = RoundedCornerShape(14.dp), color = c.primaryContainer) {
                    Text("¡${newly.size} nuevos desbloqueos! Ya están en tu colección.",
                        Modifier.fillMaxWidth().padding(14.dp), color = c.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = kind == "medal", onClick = { kind = "medal" },
                        label = { Text("Medallas ${medals.count { it.unlocked }}/${medals.size}") })
                    FilterChip(selected = kind == "collectible", onClick = { kind = "collectible" },
                        label = { Text("Cartas ${cards.count { it.unlocked }}/${cards.size}") })
                }
            }
            items(shown.chunked(2)) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { award ->
                        CollectionTile(award, award.id in newly, Modifier.weight(1f)) { selected = award }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            item {
                Text("Tu colección se guarda en tu cuenta. Las cartas de arcade reconocen partidas registradas; no son clasificaciones competitivas.",
                    color = c.onSurfaceVariant, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
    selected?.let { award ->
        AlertDialog(onDismissRequest = { selected = null },
            title = { Text(award.title, fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AwardArt(award, Modifier.fillMaxWidth().height(160.dp))
                    Text(award.description, color = c.onSurface, textAlign = TextAlign.Center)
                    Text(if (award.unlocked) "Desbloqueado · ${award.unlockedAt?.take(10).orEmpty()}" else "${award.progress} de ${award.goal} · Bloqueado",
                        color = c.onSurfaceVariant, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Cerrar") } })
    }
}

@Composable
private fun CollectionTile(award: AchievementItem, isNew: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (award.unlocked) rarityColor(award.rarity) else c.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = c.surface)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AwardArt(award, Modifier.fillMaxWidth().height(100.dp))
            Text(if (isNew) "NUEVO" else if (!award.unlocked) "BLOQUEADO" else when (award.rarity) {
                "gold" -> "ORO"; "silver" -> "PLATA"; else -> "BRONCE"
            }, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = c.onSurfaceVariant)
            Text(award.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = c.onSurface, minLines = 2, maxLines = 2)
            LinearProgressIndicator(progress = { (award.progress.toFloat() / award.goal.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(5.dp), color = c.primary, trackColor = c.surfaceVariant)
            Text(if (award.unlocked) "Conseguido" else "${award.progress}/${award.goal}", fontSize = 11.sp, color = c.onSurfaceVariant)
        }
    }
}

private fun rarityColor(rarity: String): Color = when (rarity) {
    "gold" -> Color(0xFFF6C95D)
    "silver" -> Color(0xFFB9CCE2)
    else -> Color(0xFFDAA077)
}

@Composable
private fun AwardArt(award: AchievementItem, modifier: Modifier) {
    val c = MaterialTheme.colorScheme
    val medal = award.kind == "medal"
    val metal = if (award.unlocked) rarityColor(award.rarity) else Color(0xFF8795A7)
    val base = if (award.unlocked) Color(0xFF223554) else c.surfaceVariant
    val symbol = when (award.icon) {
        "road" -> R.drawable.ic_award_road
        "target" -> R.drawable.ic_award_target
        "flame" -> R.drawable.ic_award_flame
        "gamepad" -> R.drawable.ic_home_gamepad
        "wrench" -> R.drawable.ic_award_wrench
        "traffic" -> R.drawable.ic_award_traffic
        "stop" -> R.drawable.ic_award_stop
        "wheel" -> R.drawable.ic_award_wheel
        "glove" -> R.drawable.ic_award_glove
        else -> R.drawable.ic_collection_medal
    }
    Box(modifier.background(base, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val u = minOf(size.width, size.height)
            if (medal) {
                val center = Offset(size.width / 2, size.height * .57f)
                val ribbon = Path().apply {
                    moveTo(center.x - u * .27f, u * .05f)
                    lineTo(center.x - u * .08f, u * .05f)
                    lineTo(center.x + u * .12f, u * .43f)
                    lineTo(center.x - u * .05f, u * .54f)
                    close()
                }
                drawPath(ribbon, if (award.unlocked) Color(0xFF6494F4) else Color(0xFFA6B2C2))
                val ribbonTwo = Path().apply {
                    moveTo(center.x + u * .27f, u * .05f)
                    lineTo(center.x + u * .08f, u * .05f)
                    lineTo(center.x - u * .12f, u * .43f)
                    lineTo(center.x + u * .05f, u * .54f)
                    close()
                }
                drawPath(ribbonTwo, if (award.unlocked) Color(0xFF315BC0) else Color(0xFF727F91))
                drawCircle(Color(0xFF0F1B2E).copy(alpha = .30f), u * .32f, center + Offset(0f, u * .035f))
                drawCircle(metal, u * .32f, center)
                drawCircle(metal.copy(alpha = .60f), u * .26f, center, style = Stroke(u * .04f))
                drawCircle(Color.White.copy(alpha = .36f), u * .29f, center, style = Stroke(u * .018f))
            } else {
                drawRect(metal.copy(alpha = .16f), Offset(0f, size.height * .74f), Size(size.width, size.height * .26f))
                drawCircle(metal.copy(alpha = .19f), u * .42f, Offset(size.width / 2, size.height / 2))
            }
        }
        if (award.unlocked && !medal && (award.icon == "carvier" || award.icon == "pelao")) {
            Image(painterResource(if (award.icon == "carvier") R.drawable.mascot_carvier else R.drawable.mascot_mister_pelao),
                null, Modifier.fillMaxSize().padding(6.dp), contentScale = ContentScale.Fit)
        } else {
            Icon(painterResource(if (award.unlocked) symbol else R.drawable.ic_award_lock), null,
                modifier = Modifier.offset(y = if (medal) 7.dp else 0.dp).size(if (medal) 29.dp else 42.dp),
                tint = if (medal) Color(0xFF263449) else metal)
        }
    }
}
