package cl.vialapp.app.ui.leaderboard

// GUÍA DEL ARCHIVO:
// Ranking semanal. Ojo con mezclar resultados de semanas distintas.

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
import cl.vialapp.app.data.session.AuthenticatedRead
import cl.vialapp.app.data.session.readError
import androidx.compose.ui.res.painterResource
import cl.vialapp.app.R
import cl.vialapp.app.ui.theme.LocalVialAppearance
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
    mascotId: String?,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val session = remember(context) { SessionManager(context) }
    var weekOffset by remember(userKey){mutableIntStateOf(0)}
    var refresh by remember(userKey) { mutableIntStateOf(0) }
    var data by remember(userKey) { mutableStateOf<WeeklyLeaderboardResponse?>(null) }
    var loading by remember(userKey) { mutableStateOf(true) }
    var error by remember(userKey) { mutableStateOf<String?>(null) }

    LaunchedEffect(userKey, weekOffset, refresh) {
        loading = true
        error = null
        try {
            val token = session.getAccessToken()
            if (token.isNullOrBlank()) {
                error = "Inicia sesión para consultar la liga."
            } else {
                val response = AuthenticatedRead.get(session,"weekly league"){ApiClient.api.getWeeklyLeaderboard(it,weekOffset)}
                val body = response.body()
                if (response.isSuccessful && body != null && body.weekOffset==weekOffset) {
                    data = body
                } else {
                    error = if(response.isSuccessful) "Esta semana no está disponible. Actualiza o reintenta."
                        else readError(response.code())
                    response.errorBody()?.close()
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            error = readError(e)
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
            TextButton(onClick=onBack,contentPadding=PaddingValues(0.dp)){Text("‹ Volver")}
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Liga semanal",
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
                "Compite aprendiendo. Cada lunes comienza una nueva liga.",
                color = LeagueGray,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 5.dp)
            )
        }
        item(key="week_selector"){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                listOf("Esta semana","Semana anterior").forEachIndexed { index,label ->
                    if(weekOffset==index) Button(onClick={},enabled=false,modifier=Modifier.weight(1f),
                        colors=ButtonDefaults.buttonColors(disabledContainerColor=MaterialTheme.colorScheme.primaryContainer,
                            disabledContentColor=MaterialTheme.colorScheme.onPrimaryContainer)){
                        Text(label,textAlign=TextAlign.Center)
                    } else OutlinedButton(onClick={data=null;error=null;loading=true;weekOffset=index},
                        modifier=Modifier.weight(1f)){Text(label,textAlign=TextAlign.Center)}
                }
            }
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
        data?.takeIf{it.weekOffset==weekOffset}?.let { board ->
            val mine = board.myEntry
            item {
                Text(
                    "${shortDate(board.weekStart)} – ${shortDate(board.weekEnd)} · ${board.participants} participantes",
                    color = LeagueGray,
                    fontSize = 13.sp
                )
                Text(if(weekOffset==0)"Reinicio: lunes, 00:00 (Chile continental)" else "Clasificación de la semana finalizada", color = LeagueGray, fontSize = 12.sp)
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
                            Text(if(weekOffset==0)"Completa una actividad de estudio con XP para participar." else "No participaste en esta semana.", color = Color.White, fontSize = 13.sp)
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
            if (weekOffset==0 && mine != null && mine.position <= 3 && MascotReactions.isSupported(mascotId)) {
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
                            if(weekOffset==0)"Todavía no hay participantes. ¡Sé el primero en sumar XP!" else "No hubo participantes con XP de estudio esta semana.",
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
                    Text("Clasificación · ${board.entries.size} de ${board.participants}", color = LeagueDark, fontWeight = FontWeight.Bold, fontSize = 19.sp)
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
                    "Solo aparecen participantes que ganaron XP de estudio en la semana seleccionada. Desde el 16/11/2026 también cuentan la práctica inteligente y los escenarios del simulador. Los XP directos de minijuegos no cuentan en la liga. Se muestra el primer nombre o un alias.",
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


/** Compact entry above the study journey; the study journey remains the main action. */
@Composable
internal fun HomeLeagueShortcut(board:WeeklyLeaderboardResponse?,onClick:()->Unit){
    val dark=LocalVialAppearance.current.dark
    val bg=if(dark)Color(0xFF342B4F) else Color(0xFFF0EAFE)
    val ink=if(dark)Color(0xFFF1E8FF) else Color(0xFF4C3480)
    val iconBg=if(dark)Color(0xFF4E3D72) else Color(0xFFE3D5FC)
    Card(onClick=onClick,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),
        colors=CardDefaults.cardColors(containerColor=bg)){
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(11.dp)){
            Surface(color=iconBg,shape=RoundedCornerShape(13.dp)){
                Box(Modifier.size(44.dp),contentAlignment=Alignment.Center){
                    Icon(painterResource(R.drawable.ic_league),null,tint=ink,modifier=Modifier.size(26.dp))
                }
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){
                Text("Liga semanal",color=ink,fontWeight=FontWeight.ExtraBold,fontSize=16.sp)
                val mine=board?.myEntry
                Text(when {
                    mine!=null -> "Puesto #"+mine.position+" · "+mine.xp+" XP"
                    board!=null && board.participants>0 -> board.participants.toString()+" participantes · ¡Únete!"
                    board!=null -> "Nueva semana · ¡Suma XP!"
                    else -> "Consulta tu posición y el ranking"
                },color=ink,fontSize=12.sp)
            }
            Text("›",color=ink,fontSize=27.sp,fontWeight=FontWeight.Bold)
        }
    }
}
