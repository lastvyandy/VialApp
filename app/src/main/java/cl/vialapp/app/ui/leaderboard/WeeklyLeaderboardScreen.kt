package cl.vialapp.app.ui.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.data.api.ApiClient
import cl.vialapp.app.data.api.LeaderboardEntryResponse
import cl.vialapp.app.data.api.WeeklyLeaderboardResponse
import cl.vialapp.app.data.session.SessionManager
import cl.vialapp.app.ui.profile.ProfileAvatar
import cl.vialapp.app.ui.mascot.MascotReaction
import cl.vialapp.app.ui.mascot.MascotReactionCard
import cl.vialapp.app.ui.mascot.MascotReactions
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import cl.vialapp.app.ui.theme.vialColor
import cl.vialapp.app.ui.theme.VialActionBlue

private val LeagueBlue: Color
    @Composable get() = vialColor(Color(0xFF2563EB))
private val LeagueDark: Color
    @Composable get() = vialColor(Color(0xFF172033))
private val LeagueGray: Color
    @Composable get() = vialColor(Color(0xFF64748B))
private val LeagueBg: Color
    @Composable get() = vialColor(Color(0xFFF4F7FB))

@Composable
fun WeeklyLeaderboardScreen(
    userKey: String,
    mascotId: String?
) {
    val context = LocalContext.current
    val session = remember(context) { SessionManager(context) }
    var refresh by remember(userKey) { mutableIntStateOf(0) }
    var data by remember(userKey) { mutableStateOf<WeeklyLeaderboardResponse?>(null) }
    var loading by remember(userKey) { mutableStateOf(true) }
    var error by remember(userKey) { mutableStateOf<String?>(null) }

    LaunchedEffect(userKey, refresh) {
        loading = true
        error = null
        try {
            val token = session.getAccessToken()
            if (token.isNullOrBlank()) {
                error = "Inicia sesión para consultar la liga."
            } else {
                val response = ApiClient.api.getWeeklyLeaderboard("Bearer $token")
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    data = body
                } else {
                    error = "No se pudo cargar la liga. Inténtalo de nuevo."
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = "No hay conexión con el servidor."
        } finally {
            loading = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(LeagueBg),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "🏆 Liga semanal",
                    color = LeagueDark,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { refresh++ }, enabled = !loading) {
                    Text("Actualizar")
                }
            }
            Text(
                "Compite con XP de exámenes, lecciones y misiones. Cada lunes empieza una nueva liga.",
                color = LeagueGray,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 5.dp)
            )
        }
        if (loading) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }
        error?.let { message ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = vialColor(Color.White))) {
                    Column(Modifier.padding(16.dp)) {
                        Text(message, color = LeagueDark)
                        TextButton(onClick = { refresh++ }) { Text("Reintentar") }
                    }
                }
            }
        }
        data?.let { board ->
            val mine = board.myEntry
            item {
                Text(
                    "${shortDate(board.weekStart)} – ${shortDate(board.weekEnd)} · ${board.participants} participantes",
                    color = LeagueGray,
                    fontSize = 13.sp
                )
                Text("Reinicio: lunes, 00:00 (Chile continental)", color = LeagueGray, fontSize = 12.sp)
            }
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = VialActionBlue)
                ) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        Text("TU POSICIÓN", color = Color.White.copy(alpha = .8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text(
                            mine?.let { "#${it.position}  ·  ${it.xp} XP" } ?: "Aún sin posición",
                            color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold
                        )
                        if (mine == null) {
                            Text("Completa una actividad de estudio con XP para participar.", color = Color.White, fontSize = 13.sp)
                        }
                        if (mine != null) {
                            Text(mine.alias, color = Color.White.copy(alpha = .8f), fontSize = 12.sp)
                        }
                    }
                }
            }
            board.previousMedal?.let { medal ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = vialColor(Color(0xFFFFF7E2)))) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(medalEmoji(medal), fontSize = 32.sp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Medalla de la semana anterior", color = LeagueDark, fontWeight = FontWeight.Bold)
                                Text("Terminaste en el puesto #$medal", color = LeagueGray, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
            if (mine != null && mine.position <= 3 && MascotReactions.isSupported(mascotId)) {
                item {
                    MascotReactionCard(
                        mascotId = mascotId.orEmpty(),
                        reaction = MascotReaction.PODIUM
                    )
                }
            }
            if (board.entries.isEmpty() && !loading) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = vialColor(Color.White))) {
                        Text(
                            "Todavía no hay participantes. ¡Sé el primero en sumar XP!",
                            color = LeagueGray,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else if (board.entries.isNotEmpty()) {
                item {
                    Text("Podio", color = LeagueDark, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    Spacer(Modifier.height(10.dp))
                    LeaguePodium(board.entries.take(3))
                }
                item {
                    Text("Clasificación", color = LeagueDark, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                }
                items(board.entries, key = { it.position }) { entry ->
                    LeagueRow(entry)
                }
                if (mine != null && board.entries.none { it.isMe }) {
                    item {
                        Text("Tu posición", color = LeagueDark, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        LeagueRow(mine)
                    }
                }
            }
            item {
                Text(
                    "En la liga se muestra solo el primer nombre del perfil o un alias si no está disponible. Los XP directos de minijuegos no cuentan aquí; tus XP del perfil se conservan.",
                    color = LeagueGray,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun LeaguePodium(entries: List<LeaderboardEntryResponse>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = vialColor(Color.White))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            val positions = if (entries.size == 3) listOf(2, 1, 3) else entries.map { it.position }
            positions.forEach { position ->
                val entry = entries.firstOrNull { it.position == position }
                if (entry != null) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(medalEmoji(position), fontSize = 27.sp)
                        ProfileAvatar(entry.alias, entry.avatarKey, Modifier.size(52.dp))
                        Text(
                            if (entry.isMe) "Tú" else entry.alias,
                            fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = LeagueDark, textAlign = TextAlign.Center
                        )
                        Text("${entry.xp} XP", fontSize = 12.sp, color = LeagueGray, textAlign = TextAlign.Center)
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(
                                when (position) { 1 -> 72.dp; 2 -> 52.dp; else -> 38.dp }
                            ),
                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                            color = when (position) {
                                1 -> vialColor(Color(0xFFFFE38C))
                                2 -> vialColor(Color(0xFFE2E8F0))
                                else -> vialColor(Color(0xFFF4D5B7))
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("$position", color = LeagueDark, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeagueRow(entry: LeaderboardEntryResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.isMe) vialColor(Color(0xFFE5EDFF)) else vialColor(Color.White)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "#${entry.position}",
                modifier = Modifier.width(42.dp),
                color = LeagueBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            ProfileAvatar(entry.alias, entry.avatarKey, Modifier.size(42.dp))
            Text(
                if (entry.isMe) "Tú" else entry.alias,
                modifier = Modifier.weight(1f),
                color = LeagueDark,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text("${entry.xp} XP", color = LeagueBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

private fun medalEmoji(position: Int): String = when (position) {
    1 -> "🥇"
    2 -> "🥈"
    else -> "🥉"
}

private fun shortDate(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd/MM"))
}.getOrDefault(value)
